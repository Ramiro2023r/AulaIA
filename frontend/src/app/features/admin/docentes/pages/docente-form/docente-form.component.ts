import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { DocenteService } from '../../../../../core/services/docente.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-docente-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, PageHeaderComponent],
  templateUrl: './docente-form.component.html',
})
export class DocenteFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private docenteService = inject(DocenteService);
  private toast = inject(ToastService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  form!: FormGroup;
  isEdit = signal(false);
  docenteId = signal<number | null>(null);
  loading = signal(false);

  ngOnInit(): void {
    this.initForm();
    
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEdit.set(true);
        this.docenteId.set(Number(id));
        this.cargarDocente(Number(id));
        // En modo edición ocultamos/deshabilitamos credenciales en el request
        this.form.get('username')?.disable();
        this.form.get('password')?.disable();
        this.form.get('password')?.clearValidators();
      }
    });
  }

  initForm(): void {
    this.form = this.fb.group({
      nombres: ['', [Validators.required, Validators.maxLength(120)]],
      apellidos: ['', [Validators.required, Validators.maxLength(120)]],
      username: ['', [Validators.required, Validators.maxLength(100)]],
      password: ['', [Validators.required]] // Solo requerido al crear
    });
  }

  cargarDocente(id: number): void {
    this.loading.set(true);
    this.docenteService.buscarPorId(id).subscribe({
      next: (docente) => {
        this.form.patchValue({
          nombres: docente.nombres,
          apellidos: docente.apellidos,
          username: docente.username
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar docente', 'error');
        this.router.navigate(['/admin/docentes']);
      }
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    // Para no enviar username/password en edición (al deshabilitarse form.value los excluye, 
    // pero aseguramos mapeando solo los datos necesarios según la API)
    
    if (this.isEdit()) {
      const updateData = {
        nombres: this.form.get('nombres')?.value,
        apellidos: this.form.get('apellidos')?.value
      };
      
      this.docenteService.actualizar(this.docenteId()!, updateData).subscribe({
        next: () => {
          this.toast.show('Docente actualizado con éxito', 'success');
          this.router.navigate(['/admin/docentes']);
        },
        error: (err) => this.handleError(err)
      });
    } else {
      const createData = this.form.value;
      this.docenteService.crear(createData).subscribe({
        next: () => {
          this.toast.show('Docente creado con éxito', 'success');
          this.router.navigate(['/admin/docentes']);
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  private handleError(err: any): void {
    this.loading.set(false);
    const msg = err.error?.message || 'Error al guardar docente';
    this.toast.show(msg, 'error');
  }
}
