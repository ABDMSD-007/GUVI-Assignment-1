import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { LoanService } from '../../services/loan-service';
import { LoginService } from '../../services/login-service';
import { Loan } from '../../models/loan';
import { EmiTransaction } from '../../models/emi-transaction';
import { PAYMENT_MODES, PAYMENT_STATUSES } from '../../models/enums';

@Component({
  selector: 'app-loan-detail-component',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './loan-detail-component.html',
})
export class LoanDetailComponent implements OnInit {
  private readonly loanService = inject(LoanService);
  private readonly loginService = inject(LoginService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isManager = this.loginService.isManager;
  readonly isAdmin = this.loginService.isAdmin;

  readonly paymentModes = PAYMENT_MODES;
  readonly paymentStatuses = PAYMENT_STATUSES;

  readonly loan = signal<Loan | null>(null);
  readonly schedule = signal<EmiTransaction[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly actionMessage = signal('');

  loanId = 0;
  formSubmitted = false;

  emi: EmiTransaction = this.emptyEmi();

  ngOnInit(): void {
    this.loanId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadLoan();
    this.loadSchedule();
  }

  loadLoan(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.loanService.getLoan(this.loanId).subscribe({
      next: (data) => {
        this.loan.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(this.loginService.getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }

  loadSchedule(): void {
    this.loanService.getSchedule(this.loanId).subscribe({
      next: (data) => this.schedule.set(data),
      error: () => this.schedule.set([]),
    });
  }

  payEmi(isValid: boolean | null): void {
    this.formSubmitted = true;
    if (!isValid) {
      return;
    }
    this.actionMessage.set('');
    this.errorMessage.set('');
    this.loanService.payEmi(this.loanId, this.emi).subscribe({
      next: () => {
        this.actionMessage.set('EMI payment recorded.');
        this.emi = this.emptyEmi();
        this.formSubmitted = false;
        this.loadSchedule();
      },
      error: (err: HttpErrorResponse) =>
        this.errorMessage.set(this.loginService.getErrorMessage(err)),
    });
  }

  approve(): void {
    this.run(this.loanService.approve(this.loanId), 'Loan approved.');
  }

  foreclose(): void {
    this.run(this.loanService.foreclose(this.loanId), 'Loan foreclosed.');
  }

  remove(): void {
    if (!confirm('Soft-delete this loan?')) {
      return;
    }
    this.loanService.delete(this.loanId).subscribe({
      next: (msg) => {
        this.actionMessage.set(msg || 'Loan deleted.');
        this.router.navigate(['/loans']);
      },
      error: (err: HttpErrorResponse) =>
        this.errorMessage.set(this.loginService.getErrorMessage(err)),
    });
  }

  private run(source: ReturnType<LoanService['approve']>, ok: string): void {
    this.actionMessage.set('');
    this.errorMessage.set('');
    source.subscribe({
      next: (updated) => {
        this.loan.set(updated);
        this.actionMessage.set(ok);
      },
      error: (err: HttpErrorResponse) =>
        this.errorMessage.set(this.loginService.getErrorMessage(err)),
    });
  }

  private emptyEmi(): EmiTransaction {
    return {
      installmentNumber: 1,
      amountPaid: 0,
      paymentDate: new Date().toISOString().substring(0, 10),
      paymentMode: 'UPI',
      paymentStatus: 'PAID',
    };
  }
}
