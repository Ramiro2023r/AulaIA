import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JustificacionService, JustificacionResponse } from '../../../../core/services/justificacion.service';
import { ConfirmDialogComponent } from '../../components/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../../shared/components/ui/page-header/page-header.component';

@Component({
  selector: 'app-justificaciones-list',
  standalone: true,
  imports: [CommonModule, ConfirmDialogComponent, PageHeaderComponent],
  templateUrl: './justificaciones-list.component.html',
  styleUrls: ['./justificaciones-list.component.css']
})
export class JustificacionesListComponent implements OnInit {
  justificaciones = signal<JustificacionResponse[]>([]);
  loading = signal(false);
  errorMsg = signal('');
  successMsg = signal('');

  // Dialog state
  showDialog = signal(false);
  dialogTitle = signal('');
  dialogMessage = signal('');
  dialogIsDanger = signal(false);
  pendingAction: (() => void) | null = null;

  constructor(private justificacionService: JustificacionService) {}

  ngOnInit(): void {
    this.cargarJustificaciones();
  }

  cargarJustificaciones(): void {
    this.loading.set(true);
    this.justificacionService.listarTodas().subscribe({
      next: (data) => {
        this.justificaciones.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.errorMsg.set('Error al cargar justificaciones');
        this.loading.set(false);
      }
    });
  }

  confirmarAccion(justificacion: JustificacionResponse, estado: 'APROBADA' | 'RECHAZADA'): void {
    this.dialogTitle.set(estado === 'APROBADA' ? 'Aprobar Justificación' : 'Rechazar Justificación');
    this.dialogMessage.set(
      `¿Estás seguro de que deseas ${estado === 'APROBADA' ? 'aprobar' : 'rechazar'} la justificación de ${justificacion.estudianteNombre} ${justificacion.estudianteApellidos}?`
    );
    this.dialogIsDanger.set(estado === 'RECHAZADA');
    this.pendingAction = () => this.ejecutarEvaluacion(justificacion.id, estado);
    this.showDialog.set(true);
  }

  onDialogConfirmed(): void {
    this.showDialog.set(false);
    if (this.pendingAction) {
      this.pendingAction();
      this.pendingAction = null;
    }
  }

  onDialogCancelled(): void {
    this.showDialog.set(false);
    this.pendingAction = null;
  }

  private ejecutarEvaluacion(id: number, estado: 'APROBADA' | 'RECHAZADA'): void {
    this.justificacionService.evaluar(id, { estado }).subscribe({
      next: () => {
        this.successMsg.set(`Justificación ${estado.toLowerCase()} con éxito`);
        setTimeout(() => this.successMsg.set(''), 3000);
        this.cargarJustificaciones();
      },
      error: () => {
        this.errorMsg.set('Error al evaluar la justificación');
        setTimeout(() => this.errorMsg.set(''), 3000);
      }
    });
  }
}

