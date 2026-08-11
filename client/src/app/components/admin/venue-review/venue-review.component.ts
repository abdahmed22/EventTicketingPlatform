import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { VenueService } from '../../../services/venue/venue.service';
import { VenueResponse, VenueStatus } from '../../../models/venue.model';
import { ApiError } from '../../../models/api-error.model';

type StatusFilter = VenueStatus | 'ALL';

@Component({
  selector: 'app-venue-review',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './venue-review.component.html',
  styleUrl: './venue-review.component.css'
})
export class VenueReviewComponent {
  private readonly venueService = inject(VenueService);

  readonly statusOptions: StatusFilter[] = ['ALL', 'PENDING', 'APPROVED', 'REJECTED'];

  readonly venues = signal<VenueResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly statusFilter = signal<StatusFilter>('PENDING');

  readonly actioningId = signal<string | null>(null);

  constructor() {
    this.load();
  }

  setFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const status = this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as VenueStatus);

    this.venueService.listAdminVenues(status).subscribe({
      next: (res) => {
        this.venues.set(res);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load venues for review.');
        this.loading.set(false);
      }
    });
  }

  approve(id: string): void {
    this.actioningId.set(id);
    this.error.set(null);

    this.venueService.approve(id).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.load();
      },
      error: (err: unknown) => {
        this.actioningId.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to approve venue.');
      }
    });
  }

  reject(id: string): void {
    this.actioningId.set(id);
    this.error.set(null);

    this.venueService.reject(id).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.load();
      },
      error: (err: unknown) => {
        this.actioningId.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to reject venue.');
      }
    });
  }
}
