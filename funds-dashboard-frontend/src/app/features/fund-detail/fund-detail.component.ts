import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-fund-detail',
  standalone: true,
  template: '<div>Fund Detail Placeholder</div>'
})
export class FundDetailComponent {
  @Input() fundId!: string;
  @Output() closed = new EventEmitter<void>();
}
