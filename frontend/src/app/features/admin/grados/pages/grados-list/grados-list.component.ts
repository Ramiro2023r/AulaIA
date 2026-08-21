import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { GradoService, GradoResponse } from '../../../../../core/services/grado.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-grados-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, PageHeaderComponent],
  templateUrl: './grados-list.component.html',
})
export class GradosListComponent implements OnInit {
  private gradoService = inject(GradoService);
  private toast = inject(ToastService);
  private router = inject(Router);

  grados = signal<GradoResponse[]>([]);
  loading = signal(false);

  ngOnInit(): void {
    this.cargarGrados();
  }

  cargarGrados(): void {
    this.loading.set(true);
    this.gradoService.listar().subscribe({
      next: (data) => {
        // Ordenar por nivel y luego por orden
        const ordenados = data.sort((a, b) => {
          if (a.nivel !== b.nivel) return a.nivel.localeCompare(b.nivel);
          return a.orden - b.orden;
        });
        this.grados.set(ordenados);
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar grados', 'error');
        this.loading.set(false);
      }
    });
  }

  crearGrado(): void {
    this.router.navigate(['/admin/grados/nuevo']);
  }

  editarGrado(id: number, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/admin/grados', id, 'editar']);
  }

  desactivarGrado(id: number, event: Event): void {
    event.stopPropagation();
    if (confirm('¿Estás seguro de desactivar este grado?')) {
      this.gradoService.desactivar(id).subscribe({
        next: () => {
          this.toast.show('Grado desactivado con éxito', 'success');
          this.cargarGrados();
        },
        error: () => this.toast.show('Error al desactivar grado', 'error')
      });
    }
  }
}
