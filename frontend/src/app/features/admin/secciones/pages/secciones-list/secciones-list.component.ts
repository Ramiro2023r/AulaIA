import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { SeccionService, SeccionResponse } from '../../../../../core/services/seccion.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-secciones-list',
  standalone: true,
  imports: [CommonModule, RouterModule, PageHeaderComponent],
  templateUrl: './secciones-list.component.html',
})
export class SeccionesListComponent implements OnInit {
  private seccionService = inject(SeccionService);
  private toast = inject(ToastService);
  private router = inject(Router);

  secciones = signal<SeccionResponse[]>([]);
  loading = signal(false);

  ngOnInit(): void {
    this.cargarSecciones();
  }

  cargarSecciones(): void {
    this.loading.set(true);
    this.seccionService.listar().subscribe({
      next: (data) => {
        // Ordenar por grado y nombre
        const ordenados = data.sort((a, b) => {
          if (a.grado.nombre !== b.grado.nombre) return a.grado.nombre.localeCompare(b.grado.nombre);
          return a.nombre.localeCompare(b.nombre);
        });
        this.secciones.set(ordenados);
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar secciones', 'error');
        this.loading.set(false);
      }
    });
  }

  crearSeccion(): void {
    this.router.navigate(['/admin/secciones/nueva']);
  }

  editarSeccion(id: number, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/admin/secciones', id, 'editar']);
  }

  desactivarSeccion(id: number, event: Event): void {
    event.stopPropagation();
    if (confirm('¿Estás seguro de desactivar esta sección?')) {
      this.seccionService.desactivar(id).subscribe({
        next: () => {
          this.toast.show('Sección desactivada con éxito', 'success');
          this.cargarSecciones();
        },
        error: () => this.toast.show('Error al desactivar sección', 'error')
      });
    }
  }
}
