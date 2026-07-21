import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { LoanApplicationService } from '../../services/loan-application-service';
import { LoginService } from '../../services/login-service';
import { LoanApplication } from '../../models/loan-application';
import {
  LoanApplicationStatus,
  LOAN_APPLICATION_STATUSES,
} from '../../models/enums';

@Component({
  selector: 'app-underwriter-component',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './underwriter-component.html',
})
export class UnderwriterComponent implements OnInit {
  private readonly applicationService = inject(LoanApplicationService);
  private readonly loginService = inject(LoginService);

  readonly statuses = LOAN_APPLICATION_STATUSES;
  readonly applications = signal<LoanApplication[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly actionMessage = signal('');

  // '' means the pending queue (SUBMITTED + UNDER_REVIEW).
  filter: LoanApplicationStatus | '' = '';

  // Remarks keyed by application id for approve/reject.
  remarks: Record<number, string> = {};

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    const source =
      this.filter === ''
        ? this.applicationService.getPending()
        : this.applicationService.getByStatus(this.filter);
    source.subscribe({
      next: (data) => {
        this.applications.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(this.loginService.getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }

  startReview(id: number): void {
    this.run(this.applicationService.startReview(id), 'Application moved to review.');
  }

  approve(id: number): void {
    this.run(this.applicationService.approve(id, this.remarks[id]), 'Application approved.');
  }

  reject(id: number): void {
    this.run(this.applicationService.reject(id, this.remarks[id]), 'Application rejected.');
  }

  private run(source: ReturnType<LoanApplicationService['approve']>, ok: string): void {
    this.actionMessage.set('');
    this.errorMessage.set('');
    source.subscribe({
      next: () => {
        this.actionMessage.set(ok);
        this.load();
      },
      error: (err: HttpErrorResponse) =>
        this.errorMessage.set(this.loginService.getErrorMessage(err)),
    });
  }

  badgeClass(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'text-bg-success';
      case 'REJECTED':
        return 'text-bg-danger';
      case 'UNDER_REVIEW':
        return 'text-bg-warning';
      default:
        return 'text-bg-secondary';
    }
  }
}
