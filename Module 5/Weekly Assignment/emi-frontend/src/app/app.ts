import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { LoginService } from './services/login-service';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly loginService = inject(LoginService);
  // Reactive auth flag driven by the LoginService signal.
  readonly isLoggedIn = this.loginService.isLoggedIn;
  // Role-based flags so privileged nav links stay hidden from plain users.
  readonly isUnderwriter = this.loginService.isUnderwriter;
  readonly isManager = this.loginService.isManager;
  readonly isAdmin = this.loginService.isAdmin;
  readonly role = this.loginService.role;
}
