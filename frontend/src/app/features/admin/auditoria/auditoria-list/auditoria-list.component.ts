import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditoriaService, AuditoriaResponse, AuditoriaFiltros } from '../../../../core/services/auditoria.service';
import { PageHeaderComponent } from '../../../../shared/components/ui/page-header/page-header.component';

@Component({
  selector: 'app-auditoria-list',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent],
  templateUrl: './auditoria-list.component.html',
  styleUrls: ['./auditoria-list.component.css']
})
export class AuditoriaListComponent implements OnInit {
  registros = signal<AuditoriaResponse[]>([]);
  loading   = signal(false);
  errorMsg  = signal('');

  // Filtros controlados con ngModel
  filtros: AuditoriaFiltros = {
    usuario: '',
    entidad: '',
    accion: '',
    desde: '',
    hasta: ''
  };

  // Detalle expandido
  detalleAbierto = signal<number | null>(null);

  constructor(private auditoriaService: AuditoriaService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.errorMsg.set('');
    this.auditoriaService.listar(this.filtros).subscribe({
      next: (data) => {
        this.registros.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.errorMsg.set('Error al cargar los registros de auditoría.');
        this.loading.set(false);
      }
    });
  }

  limpiarFiltros(): void {
    this.filtros = { usuario: '', entidad: '', accion: '', desde: '', hasta: '' };
    this.cargar();
  }

  toggleDetalle(id: number): void {
    this.detalleAbierto.update(cur => cur === id ? null : id);
  }

  formatJson(json: string | null): string {
    if (!json) return '—';
    try { return JSON.stringify(JSON.parse(json), null, 2); }
    catch { return json; }
  }
}
