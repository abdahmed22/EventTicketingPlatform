import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { form, FormField, FormRoot, min, required } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';

import { EventService } from '../../../services/event/event.service';
import { SeatCategoryService } from '../../../services/seat-category/seat-category.service';
import { BookingService } from '../../../services/booking/booking.service';
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
  readonly Number = Number;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly seatCategoryService = inject(SeatCategoryService);
  private readonly bookingService = inject(BookingService);
  readonly authService = inject(AuthService);

  readonly event = signal<EventResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionSuccess = signal<string | null>(null);

  // Booking Reservation State
  readonly selectedSeatCategoryId = signal<string>('');
  readonly quantity = signal<number>(1);
  readonly reserving = signal<boolean>(false);

  readonly selectedCategory = computed(() => {
    const categoryId = this.selectedSeatCategoryId();
    const evt = this.event();
    if (!evt || !evt.seatCategories) return null;
    return evt.seatCategories.find((cat) => cat.id === categoryId) || null;
  });

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

    const fetch$ = this.authService.isAdmin()
      ? this.eventService.adminGetById(id)
      : this.authService.isOrganizer()
      ? this.eventService.getOrganizerById(id)
      : this.eventService.getPublicById(id);

    fetch$.subscribe({
      next: (res) => {
        this.event.set(res);
        if (res.seatCategories && res.seatCategories.length > 0 && !this.selectedSeatCategoryId()) {
          this.selectedSeatCategoryId.set(res.seatCategories[0].id);
        }
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load event details.');
        this.loading.set(false);
      }
    });
  }

  onSeatCategoryChange(categoryId: string): void {
    this.selectedSeatCategoryId.set(categoryId);
    this.quantity.set(1);
    this.error.set(null);
  }

  onQuantityChange(qty: number): void {
    this.quantity.set(qty);
    this.error.set(null);
  }

  private isReservationValid(): boolean {
    const categoryId = this.selectedSeatCategoryId();
    const qty = this.quantity();

    if (!categoryId) {
      this.error.set('Please select a seat category');
      return false;
    }

    if (!Number.isInteger(qty) || qty < 1) {
      this.error.set('Quantity must be at least 1');
      return false;
    }

    const category = this.selectedCategory();
    if (!category) {
      this.error.set('Selected seat category not found');
      return false;
    }

    if (category.availableSeats <= 0) {
      this.error.set('No seats are available');
      return false;
    }

    if (qty > category.availableSeats) {
      this.error.set('Quantity exceeds available seats');
      return false;
    }

    return true;
  }

  reserve(): void {
    if (!this.isReservationValid()) {
      return;
    }

    const evt = this.event();
    if (!evt) return;

    this.reserving.set(true);
    this.error.set(null);

    this.bookingService
      .reserve({
        eventId: evt.id,
        seatCategoryId: this.selectedSeatCategoryId(),
        quantity: this.quantity()
      })
      .subscribe({
        next: () => {
          this.reserving.set(false);
          this.router.navigate(['/bookings']);
        },
        error: (err) => {
          if (err instanceof ApiError && err.status === 409) {
            this.error.set(
              'The selected seats are no longer available. Available seat counts have been updated. Please select another quantity or seat category.'
            );
          } else {
            this.error.set(err instanceof ApiError ? err.message : 'Failed to reserve seats');
          }
          this.loadEvent(evt.id);
          this.reserving.set(false);
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
