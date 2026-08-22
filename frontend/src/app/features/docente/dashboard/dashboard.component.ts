import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PageHeaderComponent, SkeletonComponent } from '../../../shared/components/ui';
import { DashboardService, DashboardDocenteResponse } from '../../../core/services/dashboard.service';
import { SesionService } from '../../../core/services/sesion.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent, SkeletonComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  private dashboardService = inject(DashboardService);
  private sesionService = inject(SesionService);
  private router = inject(Router);

  data = signal<DashboardDocenteResponse | null>(null);
  selectedClase = signal<any | null>(null);
  isLoading = signal<boolean>(true);
  error = signal<string | null>(null);

  private intervalId: any;

  ngOnInit(): void {
    this.loadData();
    // Polling cada 10 segundos
    this.intervalId = setInterval(() => {
      this.loadData(false);
    }, 10000);
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  loadData(showLoading = true): void {
    if (showLoading && !this.data()) {
      this.isLoading.set(true);
    }
    
    this.dashboardService.obtenerResumenDocente().subscribe({
      next: (res) => {
        this.data.set(res);
        // Solo actualizar selectedClase automáticamente si no hay una seleccionada por el usuario
        // o si queremos forzar el sync.
        if (!this.selectedClase() && res.claseActual) {
          this.selectedClase.set(res.claseActual);
        }
        this.isLoading.set(false);
        this.error.set(null);
      },
      error: (err) => {
        console.error('Error cargando dashboard', err);
        if (!this.data()) {
          this.error.set('No se pudo cargar la información del dashboard.');
        }
        this.isLoading.set(false);
      }
    });
  }

  seleccionarClase(clase: any): void {
    this.selectedClase.set(clase);
  }

  abrirModoAula(sesionId: number | null): void {
    if (!sesionId) {
      console.warn('Intento de abrir sesión sin ID. Asegúrese de que la sesión esté creada.');
      return;
    }
    
    // Primero, llamamos al backend para asegurar que la sesión esté abierta
    this.sesionService.abrirSesion(sesionId).subscribe({
      next: (res) => {
        // Una vez asegurada que está abierta, navegamos al modo aula con el ID
        this.router.navigate(['/modo-aula'], { queryParams: { sesionId: sesionId } });
      },
      error: (err) => {
        console.error('Error al abrir la sesión:', err);
        // Si hay error (ej. ya está abierta, o cerrada), intentamos navegar de todos modos si lo permite el caso
        // Pero idealmente mostramos un error.
        this.error.set('Error al abrir la clase. Verifica tu conexión.');
      }
    });
  }

  cerrarClase(sesionId: number): void {
    if (confirm('¿Estás seguro de que deseas cerrar esta clase? Todos los estudiantes que no hayan registrado su asistencia serán marcados como AUSENTES.')) {
      this.sesionService.cerrarSesion(sesionId).subscribe({
        next: () => {
          this.loadData();
        },
        error: (err) => {
          console.error('Error al cerrar clase', err);
          this.error.set('Error al cerrar la clase.');
        }
      });
    }
  }
}
