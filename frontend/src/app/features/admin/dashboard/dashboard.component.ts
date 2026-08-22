import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardService, AdminDashboardResponse } from '../../../core/services/dashboard.service';
import { ToastService } from '../../../shared/services/toast.service';
import { PageHeaderComponent } from '../../../shared/components/ui/page-header/page-header.component';
import { SkeletonComponent } from '../../../shared/components/ui/skeleton/skeleton.component';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    PageHeaderComponent, 
    SkeletonComponent,
    BaseChartDirective
  ],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private toast = inject(ToastService);

  isLoading = signal(true);
  metrics = signal<AdminDashboardResponse | null>(null);

  // Doughnut Chart (Asistencia Hoy)
  public doughnutChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'bottom' }
    }
  };
  public doughnutChartData = signal<ChartData<'doughnut'>>({ labels: [], datasets: [] });
  public doughnutChartType: ChartType = 'doughnut';

  // Bar Chart (Tendencia 7 Dias)
  public barChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      y: { min: 0, max: 100 }
    },
    plugins: {
      legend: { display: false }
    }
  };
  public barChartData = signal<ChartData<'bar'>>({ labels: [], datasets: [] });
  public barChartType: ChartType = 'bar';

  ngOnInit(): void {
    this.loadMetrics();
  }

  private loadMetrics() {
    this.isLoading.set(true);
    this.dashboardService.obtenerResumenAdmin().subscribe({
      next: (data) => {
        this.metrics.set(data);
        this.setupCharts(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.toast.error('Error al cargar las métricas del panel.');
        this.isLoading.set(false);
      }
    });
  }

  private setupCharts(data: AdminDashboardResponse) {
    // Setup Doughnut
    const estados = Object.keys(data.distribucionEstadoHoy);
    const cantidades = Object.values(data.distribucionEstadoHoy);
    
    // Map colors
    const colors = estados.map(estado => {
      switch (estado) {
        case 'PRESENTE': return '#22c55e'; // success
        case 'TARDANZA': return '#f59e0b'; // warning
        case 'AUSENTE': return '#ef4444'; // error
        case 'JUSTIFICADO': return '#3b82f6'; // primary
        default: return '#9ca3af'; // gray
      }
    });

    this.doughnutChartData.set({
      labels: estados,
      datasets: [
        {
          data: cantidades,
          backgroundColor: colors,
          hoverBackgroundColor: colors
        }
      ]
    });

    // Setup Bar Chart
    const fechas = data.tendencia7Dias.map(t => {
      const date = new Date(t.fecha + 'T00:00:00'); // Force local interpretation
      return date.toLocaleDateString('es-ES', { weekday: 'short', day: 'numeric' });
    });
    const porcentajes = data.tendencia7Dias.map(t => t.porcentajeAsistencia);

    this.barChartData.set({
      labels: fechas,
      datasets: [
        {
          data: porcentajes,
          label: '% de Asistencia',
          backgroundColor: '#3b82f6',
          borderRadius: 4
        }
      ]
    });
  }

  getJustificacionBadgeClass(estado: string): string {
    switch (estado) {
      case 'APROBADA': return 'bg-success/20 text-success';
      case 'RECHAZADA': return 'bg-error/20 text-error';
      case 'PENDIENTE': return 'bg-warning/20 text-warning';
      default: return 'bg-neutral/20 text-neutral';
    }
  }
}
