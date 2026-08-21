import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private fb     = inject(FormBuilder);
  private auth   = inject(AuthService);
  private router = inject(Router);

  loading    = signal(false);
  errorMsg   = signal('');
  showPass   = signal(false);

  form = this.fb.group({
    usuario:    ['', [Validators.required]],
    contrasena: ['', [Validators.required, Validators.minLength(4)]],
    recordarme: [false],
  });

  togglePass(): void {
    this.showPass.update(v => !v);
  }

  onSubmit(): void {
    if (this.form.invalid || this.loading()) return;

    this.errorMsg.set('');
    this.loading.set(true);

    const { usuario, contrasena } = this.form.value;

    this.auth.login({ usuario: usuario!, contrasena: contrasena! }).subscribe({
      next: (res) => {
        this.loading.set(false);
        const redirect = res.user.rol === 'ADMIN'
          ? '/admin/dashboard'
          : '/docente/dashboard';
        this.router.navigate([redirect]);
      },
      error: (err) => {
        this.loading.set(false);
        const status = err?.status;
        if (status === 401 || status === 403) {
          this.errorMsg.set('Usuario o contraseña incorrectos.');
        } else {
          this.errorMsg.set('Error de conexión. Inténtalo nuevamente.');
        }
      }
    });
  }
}
