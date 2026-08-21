import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PageHeaderComponent, SkeletonComponent } from '../../../shared/components/ui';
import { DashboardService, DashboardDocenteResponse } from '../../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent, SkeletonComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  private dashboardService = inject(DashboardService);
  private router = inject(Router);

  data = signal<DashboardDocenteResponse | null>(null);
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

  abrirModoAula(): void {
    this.router.navigate(['/modo-aula']);
  }
}
