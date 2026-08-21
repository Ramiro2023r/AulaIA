import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { EstudianteService } from '../../../../../core/services/estudiante.service';
import { SeccionService, SeccionResponse } from '../../../../../core/services/seccion.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-estudiante-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, PageHeaderComponent],
  templateUrl: './estudiante-form.component.html',
})
export class EstudianteFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private estudianteService = inject(EstudianteService);
  private seccionService = inject(SeccionService);
  private toast = inject(ToastService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  form!: FormGroup;
  secciones = signal<SeccionResponse[]>([]);
  isEdit = signal(false);
  estudianteId = signal<number | null>(null);
  loading = signal(false);

  ngOnInit(): void {
    this.initForm();
    this.cargarSecciones();
    
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEdit.set(true);
        this.estudianteId.set(Number(id));
        this.cargarEstudiante(Number(id));
      }
    });
  }

  initForm(): void {
    this.form = this.fb.group({
      codigo: ['', [Validators.required, Validators.maxLength(50)]],
      nombres: ['', [Validators.required, Validators.maxLength(120)]],
      apellidos: ['', [Validators.required, Validators.maxLength(120)]],
      seccionId: [null, [Validators.required]]
    });
  }

  cargarSecciones(): void {
    this.seccionService.listar().subscribe({
      next: (data) => this.secciones.set(data),
      error: () => this.toast.show('Error al cargar secciones', 'error')
    });
  }

  cargarEstudiante(id: number): void {
    this.loading.set(true);
    this.estudianteService.buscarPorId(id).subscribe({
      next: (est) => {
        this.form.patchValue({
          codigo: est.codigo,
          nombres: est.nombres,
          apellidos: est.apellidos,
          seccionId: est.seccion.id
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar estudiante', 'error');
        this.router.navigate(['/admin/estudiantes']);
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
      ? this.estudianteService.actualizar(this.estudianteId()!, data)
      : this.estudianteService.crear(data);

    req$.subscribe({
      next: () => {
        this.toast.show(`Estudiante ${this.isEdit() ? 'actualizado' : 'creado'} con éxito`, 'success');
        this.router.navigate(['/admin/estudiantes']);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err.error?.message || 'Error al guardar estudiante';
        this.toast.show(msg, 'error');
      }
    });
  }
}
