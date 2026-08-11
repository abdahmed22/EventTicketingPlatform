import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { email, form, FormField, FormRoot, minLength, required, validate, pattern } from '@angular/forms/signals';
import { OrganizerApplicationService } from '../../../services/organizer-application/organizer-application.service';
import { ApiError } from '../../../models/api-error.model';
import { OrganizerApplicationRequest } from '../../../models/organizer-application.model';

interface OrganizerApplicationFormValue {
  name: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
  organizationName: string;
  reason: string;
}

const PHONE_PATTERN = /^\+?[0-9]{7,15}$/;

@Component({
  selector: 'app-organizer-application',
  standalone: true,
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './organizer-application.component.html',
  styleUrl: './organizer-application.component.css'
})
export class OrganizerApplicationComponent {
  private readonly organizerApplicationService = inject(OrganizerApplicationService);

  private readonly applicationModel = signal<OrganizerApplicationFormValue>({
    name: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
    organizationName: '',
    reason: ''
  });

  /** Set once submission succeeds; the template swaps the form for a confirmation view. */
  readonly submitted = signal(false);
  readonly submittedEmail = signal('');

  readonly applicationForm = form(
    this.applicationModel,
    (path) => {
      required(path.name, { message: 'Name is required' });

      required(path.email, { message: 'Email is required' });
      email(path.email, { message: 'Enter a valid email address' });

      required(path.phone, { message: 'Phone is required' });

      pattern(path.phone, PHONE_PATTERN, {
        message: 'Enter a valid phone number (7-15 digits, optionally starting with +)'
      });

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
            const request: OrganizerApplicationRequest = {
              name: value.name,
              email: value.email,
              phone: value.phone,
              password: value.password,
              ...(value.organizationName.trim() ? { organizationName: value.organizationName.trim() } : {}),
              ...(value.reason.trim() ? { reason: value.reason.trim() } : {})
            };
            const response = await firstValueFrom(this.organizerApplicationService.submit(request));
            this.submittedEmail.set(response.email);
            this.submitted.set(true);
            return;
          } catch (err) {
            const message = err instanceof ApiError ? err.message : 'Submission failed. Please try again.';
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