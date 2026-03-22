import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { FundService } from '../../core/services/fund.service';
import { AuthService } from '../../core/services/auth.service';
import { FundDetail } from '../../core/models/fund.model';
import { UserRole } from '../../core/models/user.model';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';

@Component({
  selector: 'app-fund-detail',
  standalone: true,
  imports: [FormsModule, DecimalPipe, DatePipe],
  templateUrl: './fund-detail.component.html',
  styleUrl: './fund-detail.component.css'
})
export class FundDetailComponent implements OnInit {
  @Input() fundId!: string;
  @Output() closed = new EventEmitter<void>();

  fund: FundDetail | null = null;
  editableFields: Partial<FundDetail> = {};
  loading = true;
  saving = false;
  message = '';
  role: UserRole | null = null;

  constructor(private fundService: FundService, private authService: AuthService) {}

  ngOnInit() {
    this.role = this.authService.role;
    this.fundService.getFund(this.fundId).subscribe({
      next: (fund) => {
        this.fund = fund;
        this.editableFields = { ...fund };
        this.loading = false;
      }
    });
  }

  isEditor(): boolean { return this.role === 'EDITOR'; }
  isApprover(): boolean { return this.role === 'APPROVER'; }

  save() {
    this.saving = true;
    this.fundService.save(this.fundId, this.editableFields).subscribe({
      next: () => { this.message = 'Saved successfully.'; this.saving = false; },
      error: () => { this.message = 'Save failed.'; this.saving = false; }
    });
  }

  submit() {
    this.saving = true;
    this.fundService.submit(this.fundId, this.editableFields).subscribe({
      next: () => { this.message = 'Submitted for approval.'; this.saving = false; this.close(); },
      error: () => { this.message = 'Submit failed.'; this.saving = false; }
    });
  }

  approve() {
    this.saving = true;
    this.fundService.approve(this.fundId).subscribe({
      next: () => { this.message = 'Fund approved.'; this.saving = false; this.close(); },
      error: () => { this.message = 'Approve failed.'; this.saving = false; }
    });
  }

  reject() {
    this.saving = true;
    this.fundService.reject(this.fundId).subscribe({
      next: () => { this.message = 'Fund rejected.'; this.saving = false; this.close(); },
      error: () => { this.message = 'Reject failed.'; this.saving = false; }
    });
  }

  close() { this.closed.emit(); }
}
