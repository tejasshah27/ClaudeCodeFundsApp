import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { FundService } from '../../core/services/fund.service';
import { FundDetail, FundSummary, FundStatus } from '../../core/models/fund.model';
import { UserRole } from '../../core/models/user.model';
import { DecimalPipe, DatePipe } from '@angular/common';
import { FundDetailComponent } from '../fund-detail/fund-detail.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DecimalPipe, DatePipe, FundDetailComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  funds: (FundSummary | FundDetail)[] = [];
  selectedFundId: string | null = null;
  role: UserRole | null = null;
  loading = true;

  constructor(
    private fundService: FundService,
    public authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.role = this.authService.role;
    this.loadFunds();
  }

  loadFunds() {
    this.loading = true;
    this.fundService.getFunds().subscribe({
      next: (funds) => { this.funds = funds; this.loading = false; this.cdr.detectChanges(); },
      error: () => { this.loading = false; this.cdr.detectChanges(); this.router.navigate(['/login']); }
    });
  }

  openDetail(id: string) {
    this.selectedFundId = id;
  }

  closeDetail() {
    this.selectedFundId = null;
    this.loadFunds();
  }

  logout() {
    this.authService.logout().subscribe();
  }

  isApprover(): boolean { return this.role === 'APPROVER'; }

  asFundDetail(f: FundSummary | FundDetail): FundDetail {
    return f as FundDetail;
  }

  statusClass(status: FundStatus): string {
    return `badge badge-${status.toLowerCase()}`;
  }
}
