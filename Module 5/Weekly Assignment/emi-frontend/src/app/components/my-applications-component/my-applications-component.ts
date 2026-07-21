import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { LoanApplicationService } from '../../services/loan-application-service';
import { LoginService } from '../../services/login-service';
import { LoanApplication } from '../../models/loan-application';

@Component({
  selector: 'app-my-applications-component',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-applications-component.html',
})
export class MyApplicationsComponent implements OnInit {
  private readonly applicationService = inject(LoanApplicationService);
  private readonly loginService = inject(LoginService);

  readonly applications = signal<LoanApplication[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.applicationService.getMyApplications().subscribe({
      next: (data) => {
        this.applications.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(this.loginService.getErrorMessage(err));
        this.loading.set(false);
      },
    });
  }

  badgeClass(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'text-bg-success';
      case 'REJECTED':
        return 'text-bg-danger';
      case 'UNDER_REVIEW':
        return 'text-bg-warning';
      default:
        return 'text-bg-secondary';
    }
  }
}
