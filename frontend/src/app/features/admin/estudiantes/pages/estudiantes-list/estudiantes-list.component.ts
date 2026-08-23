import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { EstudianteService, EstudianteResponse } from '../../../../../core/services/estudiante.service';
import { SeccionService, SeccionResponse } from '../../../../../core/services/seccion.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-estudiantes-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, PageHeaderComponent],
  templateUrl: './estudiantes-list.component.html',
})
export class EstudiantesListComponent implements OnInit, OnDestroy {
  private estudianteService = inject(EstudianteService);
  private seccionService = inject(SeccionService);
  private toast = inject(ToastService);
  private router = inject(Router);
  private destroy$ = new Subject<void>();

  // Subject para debounce del campo de búsqueda
  private searchSubject = new Subject<string>();

  estudiantes = signal<EstudianteResponse[]>([]);
  secciones = signal<SeccionResponse[]>([]);
  private seccionesPorId = signal<Map<number, SeccionResponse>>(new Map());
  loading = signal(false);

  // Filters
  filterQuery = signal('');
  filterSeccion = signal<number | null>(null);

  ngOnInit(): void {
    this.cargarSecciones();
    this.cargarEstudiantes();

    // Escuchar cambios en el campo de búsqueda con debounce de 350ms
    this.searchSubject.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.cargarEstudiantes();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarSecciones(): void {
    this.seccionService.listar().subscribe({
      next: (data) => {
        this.secciones.set(data);
        this.seccionesPorId.set(new Map(data.map(seccion => [seccion.id, seccion])));
      },
      error: () => this.toast.show('Error al cargar secciones', 'error')
    });
  }

  cargarEstudiantes(): void {
    this.loading.set(true);
    const query = this.filterQuery().trim();
    const sec = this.filterSeccion();

    this.estudianteService.listar({
      // 'buscar' hace OR parcial sobre código, nombres y apellidos (case-insensitive)
      buscar: query || undefined,
      seccion: sec ? Number(sec) : undefined
    }).subscribe({
      next: (data) => {
        this.estudiantes.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar estudiantes', 'error');
        this.loading.set(false);
      }
    });
  }

  /** Llamado por (ngModelChange) del input de búsqueda para filtrar mientras escribe */
  onSearchChange(value: string): void {
    this.filterQuery.set(value);
    this.searchSubject.next(value);
  }

  /** Llamado al cambiar el filtro de sección (filtro inmediato) */
  onSeccionChange(): void {
    this.cargarEstudiantes();
  }

  aplicarFiltros(): void {
    this.cargarEstudiantes();
  }

  limpiarFiltros(): void {
    this.filterQuery.set('');
    this.filterSeccion.set(null);
    this.cargarEstudiantes();
  }

  crearEstudiante(): void {
    this.router.navigate(['/admin/estudiantes/nuevo']);
  }

  verDetalle(id: number): void {
    this.router.navigate(['/admin/estudiantes', id]);
  }

  editarEstudiante(id: number, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/admin/estudiantes', id, 'editar']);
  }

  desactivarEstudiante(id: number, event: Event): void {
    event.stopPropagation();
    if (confirm('¿Estás seguro de desactivar este estudiante?')) {
      this.estudianteService.desactivar(id).subscribe({
        next: () => {
          this.toast.show('Estudiante desactivado con éxito', 'success');
          this.cargarEstudiantes();
        },
        error: () => this.toast.show('Error al desactivar estudiante', 'error')
      });
    }
  }

  etiquetaSeccion(estudiante: EstudianteResponse): string {
    const seccion = this.seccionesPorId().get(estudiante.seccion.id);
    return seccion?.grado?.nombre
      ? `${seccion.grado.nombre} — Sección ${estudiante.seccion.nombre}`
      : `Sección ${estudiante.seccion.nombre}`;
  }
}
