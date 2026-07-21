import { Routes } from '@angular/router';
import { LoginComponent } from './components/login-component/login-component';
import { WelcomeComponent } from './components/welcome-component/welcome-component';
import { LogoutComponent } from './components/logout-component/logout-component';
import { RegisterComponent } from './components/register-component/register-component';
import { LoanListComponent } from './components/loan-list-component/loan-list-component';
import { LoanDetailComponent } from './components/loan-detail-component/loan-detail-component';
import { DashboardComponent } from './components/dashboard-component/dashboard-component';
import { ReportsComponent } from './components/reports-component/reports-component';
import { LoanProductsComponent } from './components/loan-products-component/loan-products-component';
import { MyApplicationsComponent } from './components/my-applications-component/my-applications-component';
import { MyLoansComponent } from './components/my-loans-component/my-loans-component';
import { UnderwriterComponent } from './components/underwriter-component/underwriter-component';
import { PortfolioComponent } from './components/portfolio-component/portfolio-component';
import { ManageProductsComponent } from './components/manage-products-component/manage-products-component';
import { authGuard, managerGuard, underwriterGuard, adminGuard } from './guards/auth-guard';

export const routes: Routes = [
  // Public routes
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'logout', component: LogoutComponent },

  // Authenticated routes
  { path: 'welcome', component: WelcomeComponent, canActivate: [authGuard] },

  // Customer module
  { path: 'products', component: LoanProductsComponent, canActivate: [authGuard] },
  { path: 'my-applications', component: MyApplicationsComponent, canActivate: [authGuard] },
  { path: 'my-loans', component: MyLoansComponent, canActivate: [authGuard] },

  // Underwriter module
  { path: 'underwriter', component: UnderwriterComponent, canActivate: [authGuard, underwriterGuard] },

  // Manager module
  { path: 'portfolio', component: PortfolioComponent, canActivate: [authGuard, managerGuard] },

  // Admin module
  { path: 'admin/products', component: ManageProductsComponent, canActivate: [authGuard, adminGuard] },

  // Legacy loan/analytics screens
  { path: 'loans', component: LoanListComponent, canActivate: [authGuard] },
  { path: 'loans/:id', component: LoanDetailComponent, canActivate: [authGuard] },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard, managerGuard] },
  { path: 'reports', component: ReportsComponent, canActivate: [authGuard, managerGuard] },

  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
