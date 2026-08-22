import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { AsistenciaService, AsistenciaResponse, PageResponse } from '../../../../../core/services/asistencia.service';

@Component({
  selector: 'app-asistencias-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, DatePipe],
  templateUrl: './asistencias-list.component.html',
})
export class AsistenciasListComponent implements OnInit {
  private asistenciaService = inject(AsistenciaService);

  asistencias = signal<AsistenciaResponse[]>([]);
  totalElements = signal(0);
  loading = signal(true);

  // Filtros
  fechaCtrl = new FormControl(new Date().toISOString().split('T')[0]); // Fecha de hoy
  estadoCtrl = new FormControl('');

  ngOnInit(): void {
    this.cargarAsistencias();

    this.fechaCtrl.valueChanges.subscribe(() => this.cargarAsistencias());
    this.estadoCtrl.valueChanges.subscribe(() => this.cargarAsistencias());
  }

  cargarAsistencias(): void {
    this.loading.set(true);
    
    const filtros: any = {};
    if (this.fechaCtrl.value) filtros.fecha = this.fechaCtrl.value;
    if (this.estadoCtrl.value) filtros.estado = this.estadoCtrl.value;

    this.asistenciaService.listar(filtros).subscribe({
      next: (res: PageResponse<AsistenciaResponse>) => {
        this.asistencias.set(res.content);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error cargando asistencias', err);
        this.loading.set(false);
      }
    });
  }

  getEstadoClass(estado: string): string {
    switch (estado) {
      case 'PRESENTE': return 'bg-success/20 text-success';
      case 'TARDE': return 'bg-warning/20 text-warning-dark';
      case 'AUSENTE': return 'bg-error/20 text-error';
      case 'JUSTIFICADO': return 'bg-info/20 text-info';
      default: return 'bg-surface-container text-on-surface-variant';
    }
  }

  formatHora(fechaHora: string): string {
    if (!fechaHora) return '-';
    // Asume ISO string
    const date = new Date(fechaHora);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
}
