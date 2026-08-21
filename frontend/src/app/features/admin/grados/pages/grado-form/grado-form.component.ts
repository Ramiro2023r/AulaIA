import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { GradoService } from '../../../../../core/services/grado.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-grado-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, PageHeaderComponent],
  templateUrl: './grado-form.component.html',
})
export class GradoFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private gradoService = inject(GradoService);
  private toast = inject(ToastService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  form!: FormGroup;
  isEdit = signal(false);
  gradoId = signal<number | null>(null);
  loading = signal(false);

  ngOnInit(): void {
    this.initForm();
    
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEdit.set(true);
        this.gradoId.set(Number(id));
        this.cargarGrado(Number(id));
      }
    });
  }

  initForm(): void {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(50)]],
      nivel: ['', [Validators.required, Validators.maxLength(50)]],
      orden: [1, [Validators.required, Validators.min(1)]]
    });
  }

  cargarGrado(id: number): void {
    this.loading.set(true);
    this.gradoService.buscarPorId(id).subscribe({
      next: (grado) => {
        this.form.patchValue({
          nombre: grado.nombre,
          nivel: grado.nivel,
          orden: grado.orden
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar grado', 'error');
        this.router.navigate(['/admin/grados']);
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
      ? this.gradoService.actualizar(this.gradoId()!, data)
      : this.gradoService.crear(data);

    req$.subscribe({
      next: () => {
        this.toast.show(`Grado ${this.isEdit() ? 'actualizado' : 'creado'} con éxito`, 'success');
        this.router.navigate(['/admin/grados']);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err.error?.message || 'Error al guardar grado';
        this.toast.show(msg, 'error');
      }
    });
  }
}
