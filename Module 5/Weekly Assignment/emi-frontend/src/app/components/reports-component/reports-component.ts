import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { LoanService } from '../../services/loan-service';
import { LoginService } from '../../services/login-service';
import { Loan } from '../../models/loan';
import { Customer } from '../../models/customer';
import { EmiTransaction } from '../../models/emi-transaction';
import {
  BranchCollection,
  CustomerSummary,
  MonthlyCollection,
} from '../../models/reports';

type ReportKey =
  | 'branch'
  | 'top10'
  | 'summaries'
  | 'monthly'
  | 'overdue'
  | 'noPenalty'
  | 'eligible'
  | 'multiTypes'
  | 'latest'
  | 'top5'
  | 'byBranch'
  | 'minLoans';

@Component({
  selector: 'app-reports-component',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reports-component.html',
})
export class ReportsComponent {
  private readonly loanService = inject(LoanService);
  private readonly loginService = inject(LoginService);

  readonly active = signal<ReportKey>('branch');
  readonly loading = signal(false);
  readonly errorMessage = signal('');

  // Per-report result signals.
  readonly branchCollections = signal<BranchCollection[]>([]);
  readonly customerSummaries = signal<CustomerSummary[]>([]);
  readonly monthly = signal<MonthlyCollection[]>([]);
  readonly loans = signal<Loan[]>([]);
  readonly customers = signal<Customer[]>([]);
  readonly latestPayment = signal<EmiTransaction | null>(null);
  readonly top5 = signal<unknown[]>([]);

  // Inputs for parameterised reports.
  branchInput = '';
  minLoansInput = 1;

  select(key: ReportKey): void {
    this.active.set(key);
    this.errorMessage.set('');
  }

  load(key: ReportKey): void {
    this.select(key);
    this.loading.set(true);
    this.errorMessage.set('');
    const done = () => this.loading.set(false);
    const fail = (err: HttpErrorResponse) => {
      this.errorMessage.set(this.loginService.getErrorMessage(err));
      this.loading.set(false);
    };

    switch (key) {
      case 'branch':
        this.loanService.getBranchCollection().subscribe({
          next: (d) => { this.branchCollections.set(d); done(); }, error: fail });
        break;
      case 'top10':
        this.loanService.getTop10Branches().subscribe({
          next: (d) => { this.branchCollections.set(d); done(); }, error: fail });
        break;
      case 'summaries':
        this.loanService.getCustomerSummaries().subscribe({
          next: (d) => { this.customerSummaries.set(d); done(); }, error: fail });
        break;
      case 'monthly':
        this.loanService.getMonthlyReport().subscribe({
          next: (d) => { this.monthly.set(d); done(); }, error: fail });
        break;
      case 'overdue':
        this.loanService.getOverdueLoans().subscribe({
          next: (d) => { this.loans.set(d); done(); }, error: fail });
        break;
      case 'noPenalty':
        this.loanService.getLoansWithoutPenalty().subscribe({
          next: (d) => { this.loans.set(d); done(); }, error: fail });
        break;
      case 'eligible':
        this.loanService.getEligibleCustomers().subscribe({
          next: (d) => { this.customers.set(d); done(); }, error: fail });
        break;
      case 'multiTypes':
        this.loanService.getCustomersWithMultipleLoanTypes().subscribe({
          next: (d) => { this.customers.set(d); done(); }, error: fail });
        break;
      case 'latest':
        this.loanService.getLatestPayment().subscribe({
          next: (d) => { this.latestPayment.set(d); done(); }, error: fail });
        break;
      case 'top5':
        this.loanService.getTop5CustomersByEmi().subscribe({
          next: (d) => { this.top5.set(d); done(); }, error: fail });
        break;
      case 'byBranch':
        this.loanService.getCustomersByBranch(this.branchInput).subscribe({
          next: (d) => { this.customers.set(d); done(); }, error: fail });
        break;
      case 'minLoans':
        this.loanService.getCustomersWithMinLoans(this.minLoansInput).subscribe({
          next: (d) => { this.customers.set(d); done(); }, error: fail });
        break;
    }
  }
}
