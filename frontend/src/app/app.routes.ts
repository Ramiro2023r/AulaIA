import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout.component';
import { DocenteLayoutComponent } from './layouts/docente-layout/docente-layout.component';
import { authGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Redirect root → login
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // ─── Login (full-page, sin layout) ───────────────────────────────────────
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },

  // ─── Admin (protegido por auth + rol ADMIN) ───────────────────────────────
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/admin/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'estudiantes',
        children: [
          {
            path: '',
            loadComponent: () => import('./features/admin/estudiantes/pages/estudiantes-list/estudiantes-list.component').then(m => m.EstudiantesListComponent)
          },
          {
            path: 'nuevo',
            loadComponent: () => import('./features/admin/estudiantes/pages/estudiante-form/estudiante-form.component').then(m => m.EstudianteFormComponent)
          },
          {
            path: ':id',
            loadComponent: () => import('./features/admin/estudiantes/pages/estudiante-detalle/estudiante-detalle.component').then(m => m.EstudianteDetalleComponent)
          },
          {
            path: ':id/editar',
            loadComponent: () => import('./features/admin/estudiantes/pages/estudiante-form/estudiante-form.component').then(m => m.EstudianteFormComponent)
          }
        ]
      },
      {
        path: 'grados',
        children: [
          {
            path: '',
            loadComponent: () => import('./features/admin/grados/pages/grados-list/grados-list.component').then(m => m.GradosListComponent)
          },
          {
            path: 'nuevo',
            loadComponent: () => import('./features/admin/grados/pages/grado-form/grado-form.component').then(m => m.GradoFormComponent)
          },
          {
            path: ':id/editar',
            loadComponent: () => import('./features/admin/grados/pages/grado-form/grado-form.component').then(m => m.GradoFormComponent)
          }
        ]
      },
      {
        path: 'secciones',
        children: [
          {
            path: '',
            loadComponent: () => import('./features/admin/secciones/pages/secciones-list/secciones-list.component').then(m => m.SeccionesListComponent)
          },
          {
            path: 'nueva',
            loadComponent: () => import('./features/admin/secciones/pages/seccion-form/seccion-form.component').then(m => m.SeccionFormComponent)
          },
          {
            path: ':id/editar',
            loadComponent: () => import('./features/admin/secciones/pages/seccion-form/seccion-form.component').then(m => m.SeccionFormComponent)
          }
        ]
      },
      {
        path: 'cursos',
        children: [
          {
            path: '',
            loadComponent: () => import('./features/admin/cursos/pages/cursos-list/cursos-list.component').then(m => m.CursosListComponent)
          },
          {
            path: 'nuevo',
            loadComponent: () => import('./features/admin/cursos/pages/curso-form/curso-form.component').then(m => m.CursoFormComponent)
          },
          {
            path: ':id/editar',
            loadComponent: () => import('./features/admin/cursos/pages/curso-form/curso-form.component').then(m => m.CursoFormComponent)
          }
        ]
      },
      {
        path: 'docentes',
        children: [
          {
            path: '',
            loadComponent: () => import('./features/admin/docentes/pages/docentes-list/docentes-list.component').then(m => m.DocentesListComponent)
          },
          {
            path: 'nuevo',
            loadComponent: () => import('./features/admin/docentes/pages/docente-form/docente-form.component').then(m => m.DocenteFormComponent)
          },
          {
            path: ':id/editar',
            loadComponent: () => import('./features/admin/docentes/pages/docente-form/docente-form.component').then(m => m.DocenteFormComponent)
          }
        ]
      },
      {
        path: 'horarios',
        children: [
          {
            path: '',
            loadComponent: () => import('./features/admin/horarios/pages/horarios-list/horarios-list.component').then(m => m.HorariosListComponent)
          },
          {
            path: 'nuevo',
            loadComponent: () => import('./features/admin/horarios/pages/horario-form/horario-form.component').then(m => m.HorarioFormComponent)
          },
          {
            path: ':id/editar',
            loadComponent: () => import('./features/admin/horarios/pages/horario-form/horario-form.component').then(m => m.HorarioFormComponent)
          }
        ]
      },
      {
        path: 'asistencias',
        loadComponent: () =>
          import('./features/admin/asistencias/pages/asistencias-list/asistencias-list.component').then(m => m.AsistenciasListComponent)
      },
      {
        path: 'justificaciones',
        loadComponent: () =>
          import('./features/justificaciones/pages/justificaciones-list/justificaciones-list.component').then(m => m.JustificacionesListComponent),
      },
      {
        path: 'auditoria',
        loadComponent: () =>
          import('./features/admin/auditoria/auditoria-list/auditoria-list.component').then(m => m.AuditoriaListComponent),
      },
      {
        path: 'reportes',
        loadComponent: () =>
          import('./features/admin/reportes/reportes.component').then(m => m.ReportesComponent),
      },
      {
        path: 'ia',
        loadComponent: () =>
          import('./features/ia/ia-asistente/ia-asistente.component').then(m => m.IaAsistenteComponent),
      },
      {
        path: 'ajustes',
        loadComponent: () =>
          import('./features/admin/ajustes/ajustes.component').then(m => m.AjustesComponent),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  // ─── Docente (protegido por auth + rol DOCENTE) ───────────────────────────
  {
    path: 'docente',
    component: DocenteLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DOCENTE'] },
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/docente/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'justificaciones',
        loadComponent: () =>
          import('./features/justificaciones/pages/justificaciones-list/justificaciones-list.component').then(m => m.JustificacionesListComponent),
      },
      {
        path: 'ia',
        loadComponent: () =>
          import('./features/ia/ia-asistente/ia-asistente.component').then(m => m.IaAsistenteComponent),
      },
      {
        path: 'reportes',
        loadComponent: () =>
          import('./features/docente/reportes/reportes.component').then(m => m.ReportesComponent),
      },
      {
        path: 'configuracion',
        loadComponent: () =>
          import('./features/docente/configuracion/configuracion.component').then(m => m.ConfiguracionComponent),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  // ─── Modo Aula (full-screen, solo autenticados) ───────────────────────────
  {
    path: 'modo-aula',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/aula/modo-aula/modo-aula.component').then(m => m.ModoAulaComponent),
  },

  // Fallback
  { path: '**', redirectTo: 'login' },
];
