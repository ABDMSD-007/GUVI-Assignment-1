import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { LoginService } from '../../services/login-service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-welcome-component',
  imports: [RouterLink],
  templateUrl: './welcome-component.html',
  styleUrl: './welcome-component.css',
})
export class WelcomeComponent implements OnInit {
  loginService: LoginService = inject(LoginService);
  router: Router = inject(Router);
  // Role flags so each module tile is shown only to the roles that can use it.
  readonly isUnderwriter = this.loginService.isUnderwriter;
  readonly isManager = this.loginService.isManager;
  readonly isAdmin = this.loginService.isAdmin;
  // Signals the template listens to
  greeting = signal('');
  errorMessage = signal('');

  ngOnInit(): void {
    if (localStorage.getItem('token') == null) {
      this.router.navigate(['/login']);
      return;
    }
    // Verifies the token against a protected endpoint on load
    this.loginService.getUserGreeting().subscribe({
      next: (message) => this.greeting.set(message),
      error: (err: HttpErrorResponse) => {
        console.error('Failed to load protected resource:', err);
        this.errorMessage.set(this.loginService.getErrorMessage(err));
      },
    });
  }
}
