import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const PUBLIC_AUTH_PATHS = ['/api/v1/auth/login'];

function isPublicAuthRequest(url: string): boolean {
  const path = new URL(url, 'http://aulaia.local').pathname;
  return PUBLIC_AUTH_PATHS.includes(path);
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Un login debe validar sus credenciales sin reutilizar un JWT previo.
  if (isPublicAuthRequest(req.url)) {
    return next(req);
  }

  const token = authService.getToken();

  const request = token
    ? req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    })
    : req;

  return next(request).pipe(
    catchError((error: unknown) => {
      // Un 401 de una ruta protegida con sesión almacenada indica token
      // vencido o inválido. Se limpia antes de volver al login.
      if (error instanceof HttpErrorResponse && error.status === 401 && authService.getToken()) {
        authService.clearSession();
        if (router.url !== '/login') {
          void router.navigate(['/login']);
        }
      }
      return throwError(() => error);
    })
  );
};
