import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './button.component.html',
})
export class ButtonComponent {
  @Input() variant: ButtonVariant = 'primary';
  @Input() size: ButtonSize = 'md';
  @Input() disabled: boolean = false;
  @Input() loading: boolean = false;
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input() icon?: string;
  @Input() fullWidth: boolean = false;

  get classes(): string {
    const base = 'inline-flex items-center justify-center font-label-md rounded-xl transition-colors focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed';
    
    const sizes = {
      'sm': 'px-4 py-2 h-9',
      'md': 'px-6 py-2.5 h-11',
      'lg': 'px-8 py-3 h-14 text-body-lg'
    };

    const variants = {
      'primary': 'bg-primary text-on-primary hover:bg-primary/90',
      'secondary': 'bg-secondary text-on-secondary hover:bg-secondary/90',
      'outline': 'border border-outline-variant text-on-surface hover:bg-surface-variant',
      'ghost': 'text-on-surface hover:bg-surface-variant',
      'danger': 'bg-error text-on-error hover:bg-error/90'
    };

    const width = this.fullWidth ? 'w-full' : '';

    return `${base} ${sizes[this.size]} ${variants[this.variant]} ${width}`;
  }
}
