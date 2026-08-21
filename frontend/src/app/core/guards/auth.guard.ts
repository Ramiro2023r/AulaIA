import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * AuthGuard: redirige a /login si el usuario NO está autenticado.
 */
export const authGuard: CanActivateFn = (_route, _state) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};

/**
 * RoleGuard: verifica que el usuario tenga el rol requerido.
 * Usar con data: { roles: ['ADMIN'] } en la ruta.
 */
export const roleGuard: CanActivateFn = (route, _state) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }

  const requiredRoles: string[] = route.data?.['roles'] ?? [];
  if (requiredRoles.length === 0) {
    return true; // sin restricción de rol
  }

  const userRole = auth.role();
  if (userRole && requiredRoles.includes(userRole)) {
    return true;
  }

  // Redirigir al dashboard por rol
  const redirect = auth.role() === 'DOCENTE' ? '/docente/dashboard' : '/admin/dashboard';
  return router.createUrlTree([redirect]);
};
