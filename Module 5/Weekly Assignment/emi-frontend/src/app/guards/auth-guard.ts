import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { LoginService } from '../services/login-service';

// Blocks protected routes for unauthenticated users and redirects to /login.
export const authGuard: CanActivateFn = () => {
  const loginService = inject(LoginService);
  const router = inject(Router);

  if (loginService.isLoggedIn()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};

// Restricts a route to MANAGER/ADMIN users; others are sent to the loans list.
export const managerGuard: CanActivateFn = () => {
  const loginService = inject(LoginService);
  const router = inject(Router);

  if (!loginService.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }
  if (loginService.isManager()) {
    return true;
  }
  return router.createUrlTree(['/loans']);
};

// Restricts a route to UNDERWRITER and above.
export const underwriterGuard: CanActivateFn = () => {
  const loginService = inject(LoginService);
  const router = inject(Router);

  if (!loginService.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }
  if (loginService.isUnderwriter()) {
    return true;
  }
  return router.createUrlTree(['/welcome']);
};

// Restricts a route to ADMIN only.
export const adminGuard: CanActivateFn = () => {
  const loginService = inject(LoginService);
  const router = inject(Router);

  if (!loginService.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }
  if (loginService.isAdmin()) {
    return true;
  }
  return router.createUrlTree(['/welcome']);
};
