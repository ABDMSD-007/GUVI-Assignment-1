import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { LoanProductService } from '../../services/loan-product-service';
import { LoanApplicationService } from '../../services/loan-application-service';
import { LoginService } from '../../services/login-service';
import { LoanProduct } from '../../models/loan-product';
import { LoanApplicationRequest } from '../../models/loan-application';

@Component({
  selector: 'app-loan-products-component',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loan-products-component.html',
})
export class LoanProductsComponent implements OnInit {
  private readonly productService = inject(LoanProductService);
  private readonly applicationService = inject(LoanApplicationService);
  private readonly loginService = inject(LoginService);

  readonly products = signal<LoanProduct[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly actionMessage = signal('');

  // Apply form state.
  readonly selected = signal<LoanProduct | null>(null);
  formSubmitted = false;
  application: LoanApplicationRequest = { loanCode: '', requestedAmount: 0, tenureMonths: 0 };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.productService.getActive().subscribe({
      next: (data) => {
        this.products.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(this.loginService.getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }

  openApply(product: LoanProduct): void {
    this.actionMessage.set('');
    this.errorMessage.set('');
    this.formSubmitted = false;
    this.selected.set(product);
    this.application = {
      loanCode: product.loanCode,
      requestedAmount: product.minimumAmount,
      tenureMonths: product.minimumTenure,
    };
  }

  cancelApply(): void {
    this.selected.set(null);
  }

  submitApply(isValid: boolean | null): void {
    this.formSubmitted = true;
    if (!isValid) {
      return;
    }
    this.errorMessage.set('');
    this.applicationService.apply(this.application).subscribe({
      next: (app) => {
        this.actionMessage.set(
          `Application #${app.applicationId} submitted for ${app.loanName ?? app.loanCode}.`,
        );
        this.selected.set(null);
      },
      error: (err: HttpErrorResponse) =>
        this.errorMessage.set(this.loginService.getErrorMessage(err)),
    });
  }
}
