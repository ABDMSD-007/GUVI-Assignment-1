import { Component, inject, OnInit } from '@angular/core';
import { LoginService } from '../../services/login-service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-logout-component',
  imports: [],
  templateUrl: './logout-component.html',
  styleUrl: './logout-component.css',
})
export class LogoutComponent implements OnInit {
  loginService: LoginService = inject(LoginService);
  router: Router = inject(Router);

  ngOnInit(): void {
    this.loginService.logout();
    this.router.navigate(['/login']);
  }
}
