import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth/auth.service';
import { ApiError, ApiErrorResponse } from '../models/api-error.model';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && authService.isLoggedIn()) {
        authService.logout();
        router.navigate(['/login']);
      }

      const body = error.error as Partial<ApiErrorResponse> | null;
      const message = body?.message ?? 'Something went wrong. Please try again.';
      const errorCode = body?.error;
      const fieldErrors = body?.fieldErrors ?? [];

      return throwError(() => new ApiError(message, errorCode, error.status, fieldErrors));
    })
  );
};