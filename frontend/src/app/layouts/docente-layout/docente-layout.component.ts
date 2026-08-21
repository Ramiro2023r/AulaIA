import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterModule } from '@angular/router';
import { SidebarComponent, NavItem } from '../sidebar/sidebar.component';
import { HeaderComponent } from '../header/header.component';
import { ToastComponent } from '../../shared/components/ui/toast/toast.component';

@Component({
  selector: 'app-docente-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterModule,
    SidebarComponent,
    HeaderComponent,
    ToastComponent,
  ],
  templateUrl: './docente-layout.component.html',
})
export class DocenteLayoutComponent {
  sidebarOpen = signal(false);
  pageTitle = signal('Dashboard Docente');

  navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/docente/dashboard' },
    { label: 'Justificaciones', icon: 'assignment_late', route: '/docente/justificaciones' },
  ];

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }
}
