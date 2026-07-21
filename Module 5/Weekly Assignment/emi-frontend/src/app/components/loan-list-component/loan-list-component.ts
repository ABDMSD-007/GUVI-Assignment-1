import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { LoanService } from '../../services/loan-service';
import { LoginService } from '../../services/login-service';
import { Loan } from '../../models/loan';
import { LoanType, LOAN_TYPES } from '../../models/enums';

@Component({
  selector: 'app-loan-list-component',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './loan-list-component.html',
})
export class LoanListComponent implements OnInit {
  private readonly loanService = inject(LoanService);
  private readonly loginService = inject(LoginService);

  readonly isManager = this.loginService.isManager;

  readonly loanTypes = LOAN_TYPES;
  readonly loans = signal<Loan[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly actionMessage = signal('');

  // Pagination state (server is 0-based).
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 10;

  // '' means "all loans" (paginated); otherwise filter by type.
  typeFilter: LoanType | '' = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    if (this.typeFilter === '') {
      this.loanService.getLoans(this.page(), this.pageSize).subscribe({
        next: (data) => {
          this.loans.set(data.content);
          this.totalPages.set(data.totalPages);
          this.totalElements.set(data.totalElements);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => this.fail(err),
      });
    } else {
      this.loanService.getByType(this.typeFilter).subscribe({
        next: (data) => {
          this.loans.set(data);
          this.totalPages.set(1);
          this.totalElements.set(data.length);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => this.fail(err),
      });
    }
  }

  onFilterChange(): void {
    this.page.set(0);
    this.load();
  }

  prev(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.load();
    }
  }

  next(): void {
    if (this.page() < this.totalPages() - 1) {
      this.page.update((p) => p + 1);
      this.load();
    }
  }

  // Manager-only bulk action; backend enforces the role.
  increaseInterest(): void {
    this.actionMessage.set('');
    this.errorMessage.set('');
    this.loanService.increaseInterest().subscribe({
      next: (msg) => {
        this.actionMessage.set(msg);
        this.load();
      },
      error: (err: HttpErrorResponse) =>
        this.errorMessage.set(this.loginService.getErrorMessage(err)),
    });
  }

  private fail(err: HttpErrorResponse): void {
    this.errorMessage.set(this.loginService.getErrorMessage(err));
    this.loading.set(false);
  }
}
