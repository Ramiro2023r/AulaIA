import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

export interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
})
export class SidebarComponent {
  @Input() isOpen = false;
  @Input() portalName = 'Admin Portal';
  @Output() closed = new EventEmitter<void>();
  private auth = inject(AuthService);
  private router = inject(Router);
  @Input() navItems: NavItem[] = [
    { label: 'Dashboard',   icon: 'dashboard',          route: '/admin/dashboard' },
    { label: 'Estudiantes', icon: 'school',              route: '/admin/estudiantes' },
    { label: 'Docentes',    icon: 'person_4',            route: '/admin/docentes' },
    { label: 'Grados',      icon: 'grade',               route: '/admin/grados' },
    { label: 'Secciones',   icon: 'meeting_room',        route: '/admin/secciones' },
    { label: 'Cursos',      icon: 'book',                route: '/admin/cursos' },
    { label: 'Horarios',    icon: 'calendar_today',      route: '/admin/horarios' },
    { label: 'Asistencias', icon: 'fact_check',          route: '/admin/asistencias' },
    { label: 'Justificaciones', icon: 'assignment_late', route: '/admin/justificaciones' },
    { label: 'Auditoría',   icon: 'manage_search',       route: '/admin/auditoria' },
    { label: 'Reportes',    icon: 'analytics',           route: '/admin/reportes' },
    { label: 'IA',          icon: 'psychology',          route: '/admin/ia' },
    { label: 'Ajustes',     icon: 'settings',            route: '/admin/ajustes' },
  ];

  close(): void {
    this.closed.emit();
  }

  onBackdropClick(): void {
    this.close();
  }

  logout(): void {
    this.auth.logout();
    this.close();
  }
}
