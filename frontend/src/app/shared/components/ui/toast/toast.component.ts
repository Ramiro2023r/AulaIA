import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '../../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.component.html',
})
export class ToastComponent {
  toastService = inject(ToastService);

  iconMap: Record<string, string> = {
    success: 'check_circle',
    error: 'cancel',
    warning: 'warning',
    info: 'info'
  };

  colorMap: Record<string, string> = {
    success: 'bg-secondary-container text-on-secondary-container border-secondary',
    error: 'bg-error-container text-on-error-container border-error',
    warning: 'bg-tertiary-container text-on-tertiary-container border-tertiary',
    info: 'bg-primary-fixed text-on-primary-fixed border-primary'
  };

  dismiss(id: number): void {
    this.toastService.dismiss(id);
  }

  trackById(_: number, item: Toast): number {
    return item.id;
  }
}
