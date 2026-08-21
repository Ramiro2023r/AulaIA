import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
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
export class EstudiantesListComponent implements OnInit {
  private estudianteService = inject(EstudianteService);
  private seccionService = inject(SeccionService);
  private toast = inject(ToastService);
  private router = inject(Router);

  estudiantes = signal<EstudianteResponse[]>([]);
  secciones = signal<SeccionResponse[]>([]);
  loading = signal(false);

  // Filters
  filterQuery = signal('');
  filterSeccion = signal<number | null>(null);

  ngOnInit(): void {
    this.cargarSecciones();
    this.cargarEstudiantes();
  }

  cargarSecciones(): void {
    this.seccionService.listar().subscribe({
      next: (data) => this.secciones.set(data),
      error: () => this.toast.show('Error al cargar secciones', 'error')
    });
  }

  cargarEstudiantes(): void {
    this.loading.set(true);
    const query = this.filterQuery();
    const sec = this.filterSeccion();

    this.estudianteService.listar({
      nombre: query || undefined,
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
}
