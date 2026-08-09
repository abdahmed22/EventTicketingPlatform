import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { form, FormField, FormRoot, min, required } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { VenueService } from '../../../services/venue/venue.service';
import { VenueCreateRequest, VenueResponse, VenueStatus } from '../../../models/venue.model';
import { ApiError } from '../../../models/api-error.model';

type StatusFilter = VenueStatus | 'ALL';

interface VenueFormValue {
  name: string;
  address: string;
  capacity: number;
}

@Component({
  selector: 'app-my-venues',
  standalone: true,
  imports: [DatePipe, FormField, FormRoot],
  templateUrl: './my-venues.component.html',
  styleUrl: './my-venues.component.css'
})
export class MyVenuesComponent {
  private readonly venueService = inject(VenueService);

  readonly statusOptions: StatusFilter[] = ['ALL', 'PENDING', 'APPROVED', 'REJECTED'];

  readonly venues = signal<VenueResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly statusFilter = signal<StatusFilter>('ALL');

  private readonly venueModel = signal<VenueFormValue>({
    name: '',
    address: '',
    capacity: 500
  });

  readonly venueForm = form(
    this.venueModel,
    (path) => {
      required(path.name, { message: 'Venue name is required' });
      required(path.address, { message: 'Address is required' });
      required(path.capacity, { message: 'Capacity is required' });
      min(path.capacity, 1, { message: 'Capacity must be greater than 0' });
    },
    {
      submission: {
        action: async (field) => {
          try {
            this.error.set(null);
            this.successMessage.set(null);
            const val = field().value();
            const req: VenueCreateRequest = {
              name: val.name,
              address: val.address,
              capacity: val.capacity
            };
            await firstValueFrom(this.venueService.submit(req));
            this.successMessage.set('Venue request submitted successfully! Pending admin approval.');
            this.loadVenues();
            this.venueModel.set({ name: '', address: '', capacity: 500 });
            return;
          } catch (err) {
            const message = err instanceof ApiError ? err.message : 'Failed to submit venue request.';
            return { kind: 'serverError', message };
          }
        }
      }
    }
  );

  constructor() {
    this.loadVenues();
  }

  setFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.loadVenues();
  }

  loadVenues(): void {
    this.loading.set(true);
    this.error.set(null);
    const status = this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as VenueStatus);

    this.venueService.listMyVenues(status).subscribe({
      next: (res) => {
        this.venues.set(res);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load venues.');
        this.loading.set(false);
      }
    });
  }
}
