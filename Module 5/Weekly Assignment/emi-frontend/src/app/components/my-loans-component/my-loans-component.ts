import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { LoanAccountService } from '../../services/loan-account-service';
import { EmiPaymentService } from '../../services/emi-payment-service';
import { LoginService } from '../../services/login-service';
import { LoanAccount } from '../../models/loan-account';
import { EmiPayment, EmiPaymentRequest } from '../../models/emi-payment';
import { PAYMENT_TYPES, PaymentType } from '../../models/enums';

@Component({
  selector: 'app-my-loans-component',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-loans-component.html',
})
export class MyLoansComponent implements OnInit {
  private readonly accountService = inject(LoanAccountService);
  private readonly emiService = inject(EmiPaymentService);
  private readonly loginService = inject(LoginService);

  readonly paymentTypes = PAYMENT_TYPES;

  readonly accounts = signal<LoanAccount[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly actionMessage = signal('');

  // Which account's EMI schedule is expanded, and its rows.
  readonly openId = signal<number | null>(null);
  readonly schedule = signal<EmiPayment[]>([]);
  readonly scheduleLoading = signal(false);

  // Pay form.
  payType: PaymentType = 'UPI';
  paying = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.accountService.getMyAccounts().subscribe({
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

  toggleSchedule(account: LoanAccount): void {
    if (this.openId() === account.loanAccountId) {
      this.openId.set(null);
      this.schedule.set([]);
      return;
    }
    this.openId.set(account.loanAccountId);
    this.loadSchedule(account.loanAccountId);
  }

  loadSchedule(accountId: number): void {
    this.scheduleLoading.set(true);
    this.emiService.getSchedule(accountId).subscribe({
      next: (data) => {
        this.schedule.set(data);
        this.scheduleLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(this.loginService.getErrorMessage(err));
        this.schedule.set([]);
        this.scheduleLoading.set(false);
      },
    });
  }

  payNext(accountId: number): void {
    this.actionMessage.set('');
    this.errorMessage.set('');
    this.paying.set(true);
    const request: EmiPaymentRequest = { paymentType: this.payType };
    this.emiService.pay(accountId, request).subscribe({
      next: (emi) => {
        this.actionMessage.set(
          `Installment #${emi.installmentNo} paid (${emi.totalPaid ?? emi.emiAmount}).`,
        );
        this.paying.set(false);
        this.loadSchedule(accountId);
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(this.loginService.getErrorMessage(err));
        this.paying.set(false);
      },
    });
  }

  hasPending(): boolean {
    return this.schedule().some((e) => e.status !== 'PAID');
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

  emiBadge(status: string): string {
    switch (status) {
      case 'PAID':
        return 'text-bg-success';
      case 'OVERDUE':
        return 'text-bg-danger';
      default:
        return 'text-bg-warning';
    }
  }
}
