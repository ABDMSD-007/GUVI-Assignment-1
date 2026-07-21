import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { LoanAccountService } from '../../services/loan-account-service';
import { LoginService } from '../../services/login-service';
import { LoanAccount } from '../../models/loan-account';
import {
  LoanAccountStatus,
  LOAN_ACCOUNT_STATUSES,
} from '../../models/enums';

@Component({
  selector: 'app-portfolio-component',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './portfolio-component.html',
})
export class PortfolioComponent implements OnInit {
  private readonly accountService = inject(LoanAccountService);
  private readonly loginService = inject(LoginService);

  readonly statuses = LOAN_ACCOUNT_STATUSES;
  readonly accounts = signal<LoanAccount[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly actionMessage = signal('');

  // '' means all accounts.
  filter: LoanAccountStatus | '' = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    const source =
      this.filter === ''
        ? this.accountService.getAll()
        : this.accountService.getByStatus(this.filter);
    source.subscribe({
      next: (data) => {
        this.accounts.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(this.loginService.getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }

  disburse(id: number): void {
    this.actionMessage.set('');
    this.errorMessage.set('');
    this.accountService.disburse(id).subscribe({
      next: (acc) => {
        this.actionMessage.set(`Loan ${acc.loanNumber} disbursed.`);
        this.load();
      },
      error: (err: HttpErrorResponse) =>
        this.errorMessage.set(this.loginService.getErrorMessage(err)),
    });
  }

  // Simple portfolio totals.
  totalApproved(): number {
    return this.accounts().reduce((sum, a) => sum + (a.approvedAmount || 0), 0);
  }

  countByStatus(status: string): number {
    return this.accounts().filter((a) => a.status === status).length;
  }

  statusBadge(status: string): string {
    switch (status) {
      case 'ACTIVE':
      case 'DISBURSED':
        return 'text-bg-success';
      case 'CLOSED':
        return 'text-bg-secondary';
      case 'APPROVED':
        return 'text-bg-info';
      default:
        return 'text-bg-light border';
    }
  }
}
