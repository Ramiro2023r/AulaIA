import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../../../shared/services/toast.service';
import { PageHeaderComponent } from '../../../shared/components/ui/page-header/page-header.component';
import { ButtonComponent } from '../../../shared/components/ui/button/button.component';
import { InputComponent } from '../../../shared/components/ui/input/input.component';
import { SkeletonComponent } from '../../../shared/components/ui/skeleton/skeleton.component';
import { environment } from '../../../../environments/environment';

interface DocenteProfile {
  id: number;
  nombres: string;
  apellidos: string;
  correoAlternativo: string;
  telefono: string;
  biografia: string;
}

@Component({
  selector: 'app-configuracion',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    PageHeaderComponent, 
    ButtonComponent, 
    InputComponent, 
    SkeletonComponent
  ],
  templateUrl: './configuracion.component.html'
})
export class ConfiguracionComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private toast = inject(ToastService);
  private apiUrl = `${environment.apiUrl}/docentes/perfil`;

  isLoading = signal(true);
  isSaving = signal(false);
  profileForm: FormGroup;

  constructor() {
    this.profileForm = this.fb.group({
      correoAlternativo: ['', [Validators.email, Validators.maxLength(100)]],
      telefono: ['', [Validators.maxLength(20)]],
      biografia: ['', [Validators.maxLength(500)]]
    });
  }

  ngOnInit() {
    this.loadProfile();
  }

  private loadProfile() {
    this.isLoading.set(true);
    this.http.get<DocenteProfile>(this.apiUrl).subscribe({
      next: (profile) => {
        this.profileForm.patchValue({
          correoAlternativo: profile.correoAlternativo || '',
          telefono: profile.telefono || '',
          biografia: profile.biografia || ''
        });
        this.isLoading.set(false);
      },
      error: () => {
        this.toast.error('No se pudo cargar tu perfil.');
        this.isLoading.set(false);
      }
    });
  }

  saveProfile() {
    if (this.profileForm.invalid) {
      this.toast.error('Revisa los campos ingresados (formulario inválido).');
      return;
    }

    this.isSaving.set(true);
    this.http.put<DocenteProfile>(this.apiUrl, this.profileForm.value).subscribe({
      next: () => {
        this.toast.success('Tus datos se guardaron correctamente.');
        this.isSaving.set(false);
        this.profileForm.markAsPristine();
      },
      error: () => {
        this.toast.error('No se pudieron guardar los cambios.');
        this.isSaving.set(false);
      }
    });
  }
}
