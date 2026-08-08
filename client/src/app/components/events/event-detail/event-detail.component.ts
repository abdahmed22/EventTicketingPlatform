import { Component, inject, signal } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { form, FormField, FormRoot, min, required } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { EventService } from '../../../services/event/event.service';
import { SeatCategoryService } from '../../../services/seat-category/seat-category.service';
import { AuthService } from '../../../services/auth/auth.service';
import { EventResponse } from '../../../models/event.model';
import { SeatCategoryCreateRequest, SeatCategorySummary } from '../../../models/seat-category.model';
import { ApiError } from '../../../models/api-error.model';

interface SeatCategoryFormValue {
  name: string;
  price: number;
  totalSeats: number;
  seatingCapacity: number;
}

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [DatePipe, CurrencyPipe, RouterLink, FormField, FormRoot],
  templateUrl: './event-detail.component.html',
  styleUrl: './event-detail.component.css'
})
export class EventDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly seatCategoryService = inject(SeatCategoryService);
  readonly authService = inject(AuthService);

  readonly event = signal<EventResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionSuccess = signal<string | null>(null);

  // Organizer seat category modal state
  readonly showSeatModal = signal(false);
  readonly submittingCategory = signal(false);

  private readonly seatModel = signal<SeatCategoryFormValue>({
    name: '',
    price: 50,
    totalSeats: 100,
    seatingCapacity: 1
  });

  readonly seatForm = form(
    this.seatModel,
    (path) => {
      required(path.name, { message: 'Category name is required' });
      required(path.price, { message: 'Price is required' });
      min(path.price, 0, { message: 'Price cannot be negative' });
      required(path.totalSeats, { message: 'Total seats is required' });
      min(path.totalSeats, 1, { message: 'Total seats must be at least 1' });
      required(path.seatingCapacity, { message: 'Seating capacity is required' });
      min(path.seatingCapacity, 1, { message: 'Seating capacity must be at least 1' });
    },
    {
      submission: {
        action: async (field) => {
          const evt = this.event();
          if (!evt) return;
          try {
            this.submittingCategory.set(true);
            const val = field().value();
            const req: SeatCategoryCreateRequest = {
              name: val.name,
              price: val.price,
              totalSeats: val.totalSeats,
              seatingCapacity: val.seatingCapacity
            };
            await firstValueFrom(this.seatCategoryService.create(evt.id, req));
            this.showSeatModal.set(false);
            this.actionSuccess.set('Seat category added successfully!');
            this.loadEvent(evt.id);
            return;
          } catch (err) {
            const msg = err instanceof ApiError ? err.message : 'Failed to create seat category.';
            return { kind: 'serverError', message: msg };
          } finally {
            this.submittingCategory.set(false);
          }
        }
      }
    }
  );

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadEvent(id);
    } else {
      this.error.set('Invalid event ID.');
      this.loading.set(false);
    }
  }

  loadEvent(id: string): void {
    this.loading.set(true);
    this.error.set(null);

    // If Admin or Organizer, load via role endpoint to view DRAFT/CANCELLED events, else public endpoint
    const fetch$ = this.authService.isAdmin()
      ? this.eventService.adminGetById(id)
      : this.authService.isOrganizer()
      ? this.eventService.getOrganizerById(id)
      : this.eventService.getPublicById(id);

    fetch$.subscribe({
      next: (res) => {
        this.event.set(res);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load event details.');
        this.loading.set(false);
      }
    });
  }

  publishEvent(): void {
    const evt = this.event();
    if (!evt) return;

    this.loading.set(true);
    this.eventService.publish(evt.id).subscribe({
      next: (updated) => {
        this.event.set(updated);
        this.actionSuccess.set('Event published successfully! It is now visible in public browse listings.');
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to publish event.');
        this.loading.set(false);
      }
    });
  }

  cancelEvent(): void {
    const evt = this.event();
    if (!evt) return;

    if (!confirm('Are you sure you want to cancel this event? This action will cascade-cancel active bookings.')) {
      return;
    }

    this.loading.set(true);
    const cancel$ = this.authService.isAdmin()
      ? this.eventService.adminCancel(evt.id)
      : this.eventService.organizerCancel(evt.id);

    cancel$.subscribe({
      next: (updated) => {
        this.event.set(updated);
        this.actionSuccess.set('Event has been cancelled.');
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to cancel event.');
        this.loading.set(false);
      }
    });
  }

  openSeatModal(): void {
    this.showSeatModal.set(true);
  }

  closeSeatModal(): void {
    this.showSeatModal.set(false);
  }

  getAvailabilityPercent(sc: SeatCategorySummary): number {
    if (!sc.totalSeats) return 0;
    return Math.round((sc.availableSeats / sc.totalSeats) * 100);
  }
}
