import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { SeccionService } from '../../../../../core/services/seccion.service';
import { GradoService, GradoResponse } from '../../../../../core/services/grado.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-seccion-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, PageHeaderComponent],
  templateUrl: './seccion-form.component.html',
})
export class SeccionFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private seccionService = inject(SeccionService);
  private gradoService = inject(GradoService);
  private toast = inject(ToastService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  form!: FormGroup;
  grados = signal<GradoResponse[]>([]);
  isEdit = signal(false);
  seccionId = signal<number | null>(null);
  loading = signal(false);

  ngOnInit(): void {
    this.initForm();
    this.cargarGrados();
    
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEdit.set(true);
        this.seccionId.set(Number(id));
        this.cargarSeccion(Number(id));
      }
    });
  }

  initForm(): void {
    this.form = this.fb.group({
      gradoId: [null, [Validators.required]],
      nombre: ['', [Validators.required, Validators.maxLength(50)]],
      periodoAcademico: ['2026', [Validators.required, Validators.maxLength(20)]]
    });
  }

  cargarGrados(): void {
    this.gradoService.listar().subscribe({
      next: (data) => {
        const activos = data.filter(g => g.activo);
        this.grados.set(activos.length > 0 ? activos : data);
      },
      error: () => this.toast.show('Error al cargar grados', 'error')
    });
  }

  cargarSeccion(id: number): void {
    this.loading.set(true);
    this.seccionService.buscarPorId(id).subscribe({
      next: (sec) => {
        this.form.patchValue({
          gradoId: sec.grado.id,
          nombre: sec.nombre,
          periodoAcademico: sec.periodoAcademico
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar sección', 'error');
        this.router.navigate(['/admin/secciones']);
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
      ? this.seccionService.actualizar(this.seccionId()!, data)
      : this.seccionService.crear(data);

    req$.subscribe({
      next: () => {
        this.toast.show(`Sección ${this.isEdit() ? 'actualizada' : 'creada'} con éxito`, 'success');
        this.router.navigate(['/admin/secciones']);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err.error?.message || 'Error al guardar sección';
        this.toast.show(msg, 'error');
      }
    });
  }
}
