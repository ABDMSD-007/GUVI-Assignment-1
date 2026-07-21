import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms'; // Required for [(ngModel)] usage
import { HttpErrorResponse } from '@angular/common/http';
import JwtRequestDTO from '../../dto/JwtRequestDTO';
import { LoginService } from '../../services/login-service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './login-component.html',
  styles: [`
    :host {
      display: block;
      width: 100%;
    }
  `]
})
export class LoginComponent implements OnInit {
  loginService: LoginService = inject(LoginService);
  // Simple data object for two-way binding mapping to backend DTO structure
  credentials: JwtRequestDTO = {
    username: '',
    password: ''
  };

  formSubmitted = false;
  hidePassword = true;
  // Signal that feeds the error banner in the template
  errorMessage = signal('');

  constructor(private router: Router) {}

  ngOnInit(): void {
    if (this.loginService.isLoggedIn()) {
      this.router.navigate(['/welcome']);
    }
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  onSubmit(isValid: boolean | null): void {
    this.formSubmitted = true;

    // Check HTML5 structural constraint validation before proceeding
    if (!isValid) {
      return;
    }

    // Clear any previous error before firing a new attempt
    this.errorMessage.set('');

    this.loginService.login(this.credentials).subscribe({
      next: (data) => {
        this.loginService.saveToken(data.token);
        this.router.navigate(['/welcome']);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Login failed:', err);
        this.errorMessage.set(this.loginService.getErrorMessage(err));
      }
    });
  }
}
