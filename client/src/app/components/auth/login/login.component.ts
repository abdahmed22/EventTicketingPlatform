import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { AuthService } from '../../../services/auth/auth.service';
import { ApiError } from '../../../models/api-error.model';

interface LoginFormValue {
  identifier: string;
  password: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private readonly loginModel = signal<LoginFormValue>({ identifier: '', password: '' });

  readonly loginForm = form(
    this.loginModel,
    (path) => {
      required(path.identifier, { message: 'Enter your email or phone number' });
      required(path.password, { message: 'Password is required' });
    },
    {
      submission: {
        action: async (field) => {
          try {
            await firstValueFrom(this.authService.login(field().value()));
            await this.router.navigate(['/']);
            return;
          } catch (err) {
            const message = err instanceof ApiError ? err.message : 'Login failed. Please try again.';
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