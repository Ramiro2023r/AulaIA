import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { CursoService, CursoResponse } from '../../../../../core/services/curso.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-cursos-list',
  standalone: true,
  imports: [CommonModule, RouterModule, PageHeaderComponent],
  templateUrl: './cursos-list.component.html',
})
export class CursosListComponent implements OnInit {
  private cursoService = inject(CursoService);
  private toast = inject(ToastService);
  private router = inject(Router);

  cursos = signal<CursoResponse[]>([]);
  loading = signal(false);

  ngOnInit(): void {
    this.cargarCursos();
  }

  cargarCursos(): void {
    this.loading.set(true);
    this.cursoService.listar().subscribe({
      next: (data) => {
        this.cursos.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar cursos', 'error');
        this.loading.set(false);
      }
    });
  }

  crearCurso(): void {
    this.router.navigate(['/admin/cursos/nuevo']);
  }

  editarCurso(id: number, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/admin/cursos', id, 'editar']);
  }

  desactivarCurso(id: number, event: Event): void {
    event.stopPropagation();
    if (confirm('¿Estás seguro de desactivar este curso?')) {
      this.cursoService.desactivar(id).subscribe({
        next: () => {
          this.toast.show('Curso desactivado con éxito', 'success');
          this.cargarCursos();
        },
        error: () => this.toast.show('Error al desactivar curso', 'error')
      });
    }
  }
}
