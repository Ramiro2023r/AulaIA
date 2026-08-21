import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './skeleton.component.html',
})
export class SkeletonComponent {
  @Input() width: string = '100%';
  @Input() height: string = '16px';
  @Input() rounded: string = 'rounded-md';
}
