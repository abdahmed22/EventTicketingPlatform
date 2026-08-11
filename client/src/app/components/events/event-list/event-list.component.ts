// ─── Person 2 Component: event-list.component.ts ──────────────────────────────
// Route: '/' (the homepage / public browse catalog)
// Displays published events in a card grid. Supports:
//   - Category filter tabs (ALL, MUSIC, SPORTS, CONFERENCE, THEATRE, OTHER)
//   - Sidebar filters: venue, date range, price range
//   - Pagination using Angular Signals
// No JWT required — this page is publicly accessible.
// ──────────────────────────────────────────────────────────────────────────────
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
 *
 * The landing page / public browse catalog. Shows all PUBLISHED events.
 *
 * Signals used ():
 *   - events, venues, loading, error            — raw data / UI state
 *   - currentPage, pageSize, totalPages, etc.  — pagination state
 *   - selectedCategory, selectedVenueId, etc.  — filter state
 *   - pagesArray (computed)                    — derived from totalPages
 *
 * Flow: constructor → loadVenues() + loadEvents() → subscribe → signal.set()
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

  /** Fixed list of category tabs rendered above the event grid */
  readonly categories: CategoryOption[] = ['ALL', 'MUSIC', 'SPORTS', 'CONFERENCE', 'THEATRE', 'OTHER'];

  // ─ Data signals ─────────────────────────────────────────────────────
  readonly events = signal<EventSummary[]>([]);    // currently visible event page
  readonly venues = signal<VenueResponse[]>([]);   // used to populate the venue filter dropdown
  readonly loading = signal(false);                // true while an HTTP request is in-flight
  readonly error = signal<string | null>(null);    // error message shown as a banner

  // ─ Pagination signals ────────────────────────────────────────────
  readonly currentPage = signal(0);               // 0-indexed current page
  readonly pageSize = signal(9);                  // 9 cards per page (3x3 grid)
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  /** Derived array of page indices e.g. [0,1,2,...] used to render page buttons */
  readonly pagesArray = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i));

  // ─ Filter signals ───────────────────────────────────────────────
  readonly selectedCategory = signal<CategoryOption>('ALL');
  readonly selectedVenueId = signal<string>('');
  readonly dateFrom = signal<string>('');
  readonly dateTo = signal<string>('');
  readonly minPrice = signal<number | null>(null);
  readonly maxPrice = signal<number | null>(null);

  constructor() {
    // Load venue list for the filter dropdown (independent of events)
    this.loadVenues();
    // Load first page of events immediately
    this.loadEvents();
  }

  /** Loads approved venues for the filter sidebar dropdown. Silently ignores errors. */
  loadVenues(): void {
    this.venueService.listApprovedVenues().subscribe({
      next: (venues) => this.venues.set(venues),
      error: () => {}
    });
  }

  /**
   * Fetches the current page of events using the active filter signals.
   * Guards against an invalid date range before sending the request.
   * Updates events, totalPages, totalElements, loading, and error signals.
   */
  loadEvents(): void {
    // Client-side date-range sanity check before hitting the API
    if (this.dateFrom() && this.dateTo() && this.dateFrom() > this.dateTo()) {
      this.error.set('Start date (Date From) cannot be after end date (Date To).');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    // Build the filter object; undefined values are omitted from the HTTP query string
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
        this.events.set(res.content);           // current page items
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

  /** Switches the active category tab and reloads page 1 of results. */
  setCategory(cat: CategoryOption): void {
    this.selectedCategory.set(cat);
    this.currentPage.set(0);  // reset to first page when filter changes
    this.loadEvents();
  }

  /** Triggered by the "Apply Filters" button; resets to page 1 and reloads. */
  applyFilters(): void {
    this.currentPage.set(0);
    this.loadEvents();
  }

  /** Clears all active filters and reloads the full event list. */
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

  /** Navigates to the specified page (bounds-checked) and reloads events. */
  changePage(newPage: number): void {
    if (newPage >= 0 && newPage < this.totalPages()) {
      this.currentPage.set(newPage);
      this.loadEvents();
    }
  }

  /**
   * Resolves a venueId to a display string for the event card.
   * Falls back gracefully if the venue list hasn't loaded yet.
   */
  getVenueName(venueId: string): string {
    const v = this.venues().find((item) => item.id === venueId);
    return v ? `${v.name} (${v.address})` : 'Venue details in event view';
  }

  /** Maps an EventCategory to its corresponding hero image path. */
  getCategoryImage(category: EventCategory): string {
    return getEventCategoryImage(category);
  }

  /** Hides the <img> element if its src fails to load (broken image fallback). */
  onImageError(event: Event): void {
    const target = event.target as HTMLImageElement;
    if (target) {
      target.style.display = 'none';
    }
  }
}
