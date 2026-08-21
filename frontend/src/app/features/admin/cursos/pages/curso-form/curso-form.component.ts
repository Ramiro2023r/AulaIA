import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { CursoService } from '../../../../../core/services/curso.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-curso-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, PageHeaderComponent],
  templateUrl: './curso-form.component.html',
})
export class CursoFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private cursoService = inject(CursoService);
  private toast = inject(ToastService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  form!: FormGroup;
  isEdit = signal(false);
  cursoId = signal<number | null>(null);
  loading = signal(false);

  ngOnInit(): void {
    this.initForm();
    
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEdit.set(true);
        this.cursoId.set(Number(id));
        this.cargarCurso(Number(id));
      }
    });
  }

  initForm(): void {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      descripcion: ['', [Validators.maxLength(255)]]
    });
  }

  cargarCurso(id: number): void {
    this.loading.set(true);
    this.cursoService.buscarPorId(id).subscribe({
      next: (curso) => {
        this.form.patchValue({
          nombre: curso.nombre,
          descripcion: curso.descripcion
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar curso', 'error');
        this.router.navigate(['/admin/cursos']);
      }
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const data = this.form.value;

    const req$ = this.isEdit() 
      ? this.cursoService.actualizar(this.cursoId()!, data)
      : this.cursoService.crear(data);

    req$.subscribe({
      next: () => {
        this.toast.show(`Curso ${this.isEdit() ? 'actualizado' : 'creado'} con éxito`, 'success');
        this.router.navigate(['/admin/cursos']);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err.error?.message || 'Error al guardar curso';
        this.toast.show(msg, 'error');
      }
    });
  }
}
