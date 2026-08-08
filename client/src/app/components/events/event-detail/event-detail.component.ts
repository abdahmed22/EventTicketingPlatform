import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { EventService } from '../../../services/event/event.service';
import { SeatCategoryService } from '../../../services/seat-category/seat-category.service';
import { BookingService } from '../../../services/booking/booking.service';

import { EventResponse } from '../../../models/event.model';
import { SeatCategorySummary } from '../../../models/seat-category.model';
import { ApiError } from '../../../models/api-error.model';

@Component({
  selector: 'app-event-detail',
  imports: [CurrencyPipe],
  templateUrl: './event-detail.component.html',
  styleUrl: './event-detail.component.css',
})
export class EventDetailComponent implements OnInit {
  readonly Number = Number;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly eventService = inject(EventService);
  private readonly seatCategoryService =
    inject(SeatCategoryService);
  private readonly bookingService =
    inject(BookingService);

  readonly event =
    signal<EventResponse | null>(null);

  readonly seatCategories =
    signal<SeatCategorySummary[]>([]);

  readonly selectedSeatCategoryId =
    signal<string>('');

  readonly quantity =
    signal<number>(1);

  readonly loading =
    signal<boolean>(false);

  readonly error =
    signal<string | null>(null);

  readonly selectedCategory =
    computed(() => {
      const categoryId =
        this.selectedSeatCategoryId();

      return this.seatCategories().find(
        category => category.id === categoryId
      );
    });

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {
    const eventId = this.getEventId();

    if (!eventId) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.eventService.getPublicById(eventId).subscribe({
      next: (event) => {
        this.event.set(event);

        this.loadSeatCategories(eventId);
      },

      error: (err) => {
        this.error.set(
          err instanceof ApiError
            ? err.message
            : 'Failed to load event'
        );

        this.loading.set(false);
      },
    });
  }

  private loadSeatCategories(eventId: string): void {
    this.seatCategoryService
      .listByEvent(eventId)
      .subscribe({
        next: (categories) => {
          this.seatCategories.set(categories);
          this.loading.set(false);
        },

        error: (err) => {
          this.error.set(
            err instanceof ApiError
              ? err.message
              : 'Failed to load seat categories'
          );

          this.loading.set(false);
        },
      });
  }

  private getEventId(): string | null {
    const eventId =
      this.route.snapshot.paramMap.get('id');

    if (!eventId) {
      this.error.set('Event ID is missing');
      return null;
    }

    return eventId;
  }

  onSeatCategoryChange(categoryId: string): void {
    this.selectedSeatCategoryId.set(categoryId);

    this.quantity.set(1);

    this.error.set(null);
  }

  onQuantityChange(quantity: number): void {
    this.quantity.set(quantity);
    this.error.set(null);
  }

  private isReservationValid(): boolean {
    const categoryId =
      this.selectedSeatCategoryId();

    const quantity =
      this.quantity();

    if (!categoryId) {
      this.error.set(
        'Please select a seat category'
      );

      return false;
    }

    if (!Number.isInteger(quantity) || quantity < 1) {
      this.error.set(
        'Quantity must be at least 1'
      );

      return false;
    }

    const category =
      this.selectedCategory();

    if (!category) {
      this.error.set(
        'Selected seat category not found'
      );

      return false;
    }

    if (category.availableSeats <= 0) {
      this.error.set(
        'No seats are available'
      );

      return false;
    }

    if (quantity > category.availableSeats) {
      this.error.set(
        'Quantity exceeds available seats'
      );

      return false;
    }

    return true;
  }

  reserve(): void {
    if (!this.isReservationValid()) {
      return;
    }

    const eventId = this.getEventId();

    if (!eventId) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.bookingService
      .reserve({
        eventId,
        seatCategoryId:
          this.selectedSeatCategoryId(),
        quantity: this.quantity(),
      })
      .subscribe({
        next: () => {
          this.router.navigate(['/bookings']);
        },

        error: (err) => {

          if (
            err instanceof ApiError &&
            err.status === 409
          ) {
            this.error.set(
              'The selected seats are no longer available. ' +
              'Available seat counts have been updated. ' +
              'Please select another quantity or seat category.'
            );
          } else {
            this.error.set(
              err instanceof ApiError
                ? err.message
                : 'Failed to reserve seats'
            );
          }

          this.loadSeatCategories(eventId);
          this.loading.set(false);
        },

        complete: () => {
          this.loading.set(false);
        },
      });
  }
}

