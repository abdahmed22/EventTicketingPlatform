import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { email, form, FormField, FormRoot, minLength, required, validate } from '@angular/forms/signals';
import { AuthService } from '../../../services/auth/auth.service';
import { ApiError } from '../../../models/api-error.model';
import { RegisterRequest } from '../../../models/auth.model';

interface RegisterFormValue {
  name: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private readonly registerModel = signal<RegisterFormValue>({
    name: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: ''
  });

  readonly registerForm = form(
    this.registerModel,
    (path) => {
      required(path.name, { message: 'Name is required' });

      required(path.email, { message: 'Email is required' });
      email(path.email, { message: 'Enter a valid email address' });

      required(path.password, { message: 'Password is required' });
      minLength(path.password, 8, { message: 'Password must be at least 8 characters' });

      required(path.confirmPassword, { message: 'Please confirm your password' });
      validate(path.confirmPassword, (ctx) => {
        if (ctx.value() !== ctx.valueOf(path.password)) {
          return { kind: 'mismatch', message: 'Passwords do not match' };
        }
        return null;
      });
    },
    {
      submission: {
        action: async (field) => {
          try {
            const value = field().value();
            const request: RegisterRequest = {
              name: value.name,
              email: value.email,
              password: value.password,
              ...(value.phone.trim() ? { phone: value.phone.trim() } : {})
            };
            await firstValueFrom(this.authService.register(request));
            await this.router.navigate(['/login']);
            return;
          } catch (err) {
            const message = err instanceof ApiError ? err.message : 'Registration failed. Please try again.';
            return { kind: 'serverError', message };
          }
        }
      }
    }
  );

  onImageError(event: Event): void {
    const target = event.target as HTMLImageElement;
    if (target) {
      target.style.display = 'none';
    }
  }
}