import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CustomerService } from '../../services/customer-service';
import { LoginService } from '../../services/login-service';
import { Customer } from '../../models/customer';
import { ROLES } from '../../models/enums';

@Component({
  selector: 'app-register-component',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register-component.html',
  styles: [`:host { display: block; width: 100%; }`],
})
export class RegisterComponent {
  private readonly customerService = inject(CustomerService);
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);

  readonly roles = ROLES;
  formSubmitted = false;
  hidePassword = true;

  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  customer: Customer = {
    customerName: '',
    email: '',
    password: '',
    mobileNumber: '',
    branchName: '',
    creditScore: undefined,
    role: 'USER',
  };

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  onSubmit(isValid: boolean | null): void {
    this.formSubmitted = true;
    if (!isValid) {
      return;
    }
    this.errorMessage.set('');
    this.successMessage.set('');

    this.customerService.register(this.customer).subscribe({
      next: () => {
        this.successMessage.set('Account created. Redirecting to sign in...');
        setTimeout(() => this.router.navigate(['/login']), 1200);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(this.loginService.getErrorMessage(err));
      },
    });
  }
}
