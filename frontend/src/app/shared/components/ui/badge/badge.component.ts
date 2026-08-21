import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type BadgeVariant = 'success' | 'warning' | 'error' | 'neutral' | 'primary';

@Component({
  selector: 'app-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './badge.component.html',
})
export class BadgeComponent {
  @Input() variant: BadgeVariant = 'neutral';
  @Input() icon?: string;

  get classes(): string {
    const base = 'inline-flex items-center px-2.5 py-0.5 rounded-full text-label-sm uppercase tracking-wider font-bold';
    
    const variants = {
      'success': 'bg-secondary-container text-on-secondary-container',
      'warning': 'bg-tertiary-container text-on-tertiary-container',
      'error': 'bg-error-container text-on-error-container',
      'neutral': 'bg-surface-variant text-on-surface-variant',
      'primary': 'bg-primary-container text-on-primary-container'
    };

    return `${base} ${variants[this.variant]}`;
  }
}
