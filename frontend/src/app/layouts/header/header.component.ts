import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.component.html',
})
export class HeaderComponent {
  @Input() pageTitle: string = '';
  @Output() menuToggled = new EventEmitter<void>();

  toggleMenu(): void {
    this.menuToggled.emit();
  }
}
