import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { DocenteService, DocenteResponse } from '../../../../../core/services/docente.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-docentes-list',
  standalone: true,
  imports: [CommonModule, RouterModule, PageHeaderComponent],
  templateUrl: './docentes-list.component.html',
})
export class DocentesListComponent implements OnInit {
  private docenteService = inject(DocenteService);
  private toast = inject(ToastService);
  private router = inject(Router);

  docentes = signal<DocenteResponse[]>([]);
  loading = signal(false);

  ngOnInit(): void {
    this.cargarDocentes();
  }

  cargarDocentes(): void {
    this.loading.set(true);
    this.docenteService.listar().subscribe({
      next: (data) => {
        const ordenados = data.sort((a, b) => a.apellidos.localeCompare(b.apellidos));
        this.docentes.set(ordenados);
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar docentes', 'error');
        this.loading.set(false);
      }
    });
  }

  crearDocente(): void {
    this.router.navigate(['/admin/docentes/nuevo']);
  }

  editarDocente(id: number, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/admin/docentes', id, 'editar']);
  }

  desactivarDocente(id: number, event: Event): void {
    event.stopPropagation();
    if (confirm('¿Estás seguro de desactivar a este docente?')) {
      this.docenteService.desactivar(id).subscribe({
        next: () => {
          this.toast.show('Docente desactivado con éxito', 'success');
          this.cargarDocentes();
        },
        error: () => this.toast.show('Error al desactivar docente', 'error')
      });
    }
  }
}
