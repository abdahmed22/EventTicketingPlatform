import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth/auth.service';
import { UserRole } from '../models/auth.model';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};

export function roleGuard(...allowedRoles: UserRole[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isLoggedIn()) {
      router.navigate(['/login']);
      return false;
    }

    if (!allowedRoles.includes(authService.role()!)) {
      router.navigate(['/']);
      return false;
    }

    return true;
  };
}

export const customerGuard: CanActivateFn = roleGuard('CUSTOMER');
export const organizerGuard: CanActivateFn = roleGuard('ORGANIZER');
export const adminGuard: CanActivateFn = roleGuard('ADMIN');
export const organizerOrAdminGuard: CanActivateFn = roleGuard('ORGANIZER', 'ADMIN');