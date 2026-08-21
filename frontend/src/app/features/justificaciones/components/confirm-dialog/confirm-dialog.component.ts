import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="overlay" (click)="onCancel()">
      <div class="dialog-panel" (click)="$event.stopPropagation()">
        <h3 class="dialog-title">{{ title }}</h3>
        <p class="dialog-message">{{ message }}</p>
        <div class="dialog-actions">
          <button class="btn btn-cancel" (click)="onCancel()">{{ cancelText }}</button>
          <button class="btn btn-confirm" [class.btn-danger]="isDanger" (click)="onConfirm()">{{ confirmText }}</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .overlay {
      position: fixed; inset: 0;
      background: rgba(0,0,0,0.5);
      display: flex; align-items: center; justify-content: center;
      z-index: 1000;
    }
    .dialog-panel {
      background: white; border-radius: 12px;
      padding: 28px; max-width: 420px; width: 90%;
      box-shadow: 0 20px 60px rgba(0,0,0,0.3);
    }
    .dialog-title { margin: 0 0 12px; font-size: 18px; font-weight: 700; color: #1e293b; }
    .dialog-message { margin: 0 0 24px; color: #475569; line-height: 1.5; }
    .dialog-actions { display: flex; gap: 12px; justify-content: flex-end; }
    .btn { padding: 10px 20px; border-radius: 8px; border: none; font-weight: 600; cursor: pointer; font-size: 14px; }
    .btn-cancel { background: #f1f5f9; color: #475569; }
    .btn-cancel:hover { background: #e2e8f0; }
    .btn-confirm { background: #3b82f6; color: white; }
    .btn-confirm:hover { background: #2563eb; }
    .btn-danger { background: #ef4444 !important; }
    .btn-danger:hover { background: #dc2626 !important; }
  `]
})
export class ConfirmDialogComponent {
  @Input() title = 'Confirmar acción';
  @Input() message = '¿Estás seguro?';
  @Input() confirmText = 'Confirmar';
  @Input() cancelText = 'Cancelar';
  @Input() isDanger = false;
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  onConfirm(): void { this.confirmed.emit(); }
  onCancel(): void { this.cancelled.emit(); }
}
