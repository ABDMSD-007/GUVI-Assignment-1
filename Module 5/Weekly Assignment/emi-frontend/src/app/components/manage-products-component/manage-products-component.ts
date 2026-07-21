import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { LoanProductService } from '../../services/loan-product-service';
import { LoginService } from '../../services/login-service';
import { LoanProduct, LoanProductRequest } from '../../models/loan-product';
import { LOAN_TYPES } from '../../models/enums';

@Component({
  selector: 'app-manage-products-component',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './manage-products-component.html',
})
export class ManageProductsComponent implements OnInit {
  private readonly productService = inject(LoanProductService);
  private readonly loginService = inject(LoginService);

  readonly loanTypes = LOAN_TYPES;
  readonly products = signal<LoanProduct[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly actionMessage = signal('');

  readonly editing = signal(false);
  readonly isNew = signal(true);
  formSubmitted = false;
  form: LoanProductRequest = this.emptyForm();

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.productService.getAll().subscribe({
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

  newProduct(): void {
    this.isNew.set(true);
    this.editing.set(true);
    this.formSubmitted = false;
    this.form = this.emptyForm();
  }

  edit(product: LoanProduct): void {
    this.isNew.set(false);
    this.editing.set(true);
    this.formSubmitted = false;
    this.form = {
      loanCode: product.loanCode,
      loanName: product.loanName,
      loanType: product.loanType,
      minimumAmount: product.minimumAmount,
      maximumAmount: product.maximumAmount,
      interestRate: product.interestRate,
      minimumTenure: product.minimumTenure,
      maximumTenure: product.maximumTenure,
      processingFee: product.processingFee,
      dailyPenaltyRate: product.dailyPenaltyRate,
      active: product.active,
    };
  }

  cancel(): void {
    this.editing.set(false);
  }

  save(isValid: boolean | null): void {
    this.formSubmitted = true;
    if (!isValid) {
      return;
    }
    this.actionMessage.set('');
    this.errorMessage.set('');
    const request$ = this.isNew()
      ? this.productService.create(this.form)
      : this.productService.update(this.form.loanCode, this.form);
    request$.subscribe({
      next: (p) => {
        this.actionMessage.set(
          `${this.isNew() ? 'Created' : 'Updated'} product ${p.loanCode}.`,
        );
        this.editing.set(false);
        this.load();
      },
      error: (err: HttpErrorResponse) =>
        this.errorMessage.set(this.loginService.getErrorMessage(err)),
    });
  }

  deactivate(product: LoanProduct): void {
    if (!confirm(`Deactivate product ${product.loanCode}?`)) {
      return;
    }
    this.actionMessage.set('');
    this.errorMessage.set('');
    this.productService.deactivate(product.loanCode).subscribe({
      next: (msg) => {
        this.actionMessage.set(msg || 'Product deactivated.');
        this.load();
      },
      error: (err: HttpErrorResponse) =>
        this.errorMessage.set(this.loginService.getErrorMessage(err)),
    });
  }

  private emptyForm(): LoanProductRequest {
    return {
      loanCode: '',
      loanName: '',
      loanType: 'PERSONAL',
      minimumAmount: 0,
      maximumAmount: 0,
      interestRate: 0,
      minimumTenure: 1,
      maximumTenure: 12,
      processingFee: 0,
      dailyPenaltyRate: 0,
      active: true,
    };
  }
}
