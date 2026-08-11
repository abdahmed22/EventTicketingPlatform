import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EventService } from '../../../services/event/event.service';
import { VenueService } from '../../../services/venue/venue.service';
import { EventCategory, EventFilterParams, EventSummary } from '../../../models/event.model';
import { VenueResponse } from '../../../models/venue.model';
import { ApiError } from '../../../models/api-error.model';
import { getEventCategoryImage } from '../../../utils/event-image.util';

type CategoryOption = EventCategory | 'ALL';

/**
 * Person 2 Component: EventListComponent
 * Displays public browse catalog of published events with category tabs,
 * filters (venue, date range, price range), and Angular Signals.
 */
@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './event-list.component.html',
  styleUrl: './event-list.component.css'
})
export class EventListComponent {
  private readonly eventService = inject(EventService);
  private readonly venueService = inject(VenueService);

  readonly categories: CategoryOption[] = ['ALL', 'MUSIC', 'SPORTS', 'CONFERENCE', 'THEATRE', 'OTHER'];

  readonly events = signal<EventSummary[]>([]);
  readonly venues = signal<VenueResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  // Pagination signals
  readonly currentPage = signal(0);
  readonly pageSize = signal(9);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pagesArray = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i));

  // Filter signals
  readonly selectedCategory = signal<CategoryOption>('ALL');
  readonly selectedVenueId = signal<string>('');
  readonly dateFrom = signal<string>('');
  readonly dateTo = signal<string>('');
  readonly minPrice = signal<number | null>(null);
  readonly maxPrice = signal<number | null>(null);

  constructor() {
    this.loadVenues();
    this.loadEvents();
  }

  loadVenues(): void {
    this.venueService.listApprovedVenues().subscribe({
      next: (venues) => this.venues.set(venues),
      error: () => {}
    });
  }

  loadEvents(): void {
    if (this.dateFrom() && this.dateTo() && this.dateFrom() > this.dateTo()) {
      this.error.set('Start date (Date From) cannot be after end date (Date To).');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const filters: EventFilterParams = {
      category: this.selectedCategory() === 'ALL' ? undefined : (this.selectedCategory() as EventCategory),
      venueId: this.selectedVenueId() || undefined,
      dateFrom: this.dateFrom() || undefined,
      dateTo: this.dateTo() || undefined,
      minPrice: this.minPrice() !== null && !isNaN(this.minPrice()!) ? this.minPrice()! : undefined,
      maxPrice: this.maxPrice() !== null && !isNaN(this.maxPrice()!) ? this.maxPrice()! : undefined,
      page: this.currentPage(),
      size: this.pageSize()
    };

    this.eventService.browsePublished(filters).subscribe({
      next: (res) => {
        this.events.set(res.content);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load events.');
        this.loading.set(false);
      }
    });
  }

  setCategory(cat: CategoryOption): void {
    this.selectedCategory.set(cat);
    this.currentPage.set(0);
    this.loadEvents();
  }

  applyFilters(): void {
    this.currentPage.set(0);
    this.loadEvents();
  }

  resetFilters(): void {
    this.selectedCategory.set('ALL');
    this.selectedVenueId.set('');
    this.dateFrom.set('');
    this.dateTo.set('');
    this.minPrice.set(null);
    this.maxPrice.set(null);
    this.currentPage.set(0);
    this.loadEvents();
  }

  changePage(newPage: number): void {
    if (newPage >= 0 && newPage < this.totalPages()) {
      this.currentPage.set(newPage);
      this.loadEvents();
    }
  }

  getVenueName(venueId: string): string {
    const v = this.venues().find((item) => item.id === venueId);
    return v ? `${v.name} (${v.address})` : 'Venue details in event view';
  }

  getCategoryImage(category: EventCategory): string {
    return getEventCategoryImage(category);
  }

  onImageError(event: Event): void {
    const target = event.target as HTMLImageElement;
    if (target) {
      target.style.display = 'none';
    }
  }
}
