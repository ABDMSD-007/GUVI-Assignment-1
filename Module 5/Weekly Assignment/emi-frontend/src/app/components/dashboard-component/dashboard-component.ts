import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { DashboardService } from '../../services/dashboard-service';
import { LoginService } from '../../services/login-service';
import { DashboardDto } from '../../models/dashboard';

@Component({
  selector: 'app-dashboard-component',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-component.html',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly loginService = inject(LoginService);

  readonly data = signal<DashboardDto | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal('');

  ngOnInit(): void {
    this.loading.set(true);
    this.dashboardService.getDashboard().subscribe({
      next: (dto) => {
        this.data.set(dto);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(this.loginService.getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
