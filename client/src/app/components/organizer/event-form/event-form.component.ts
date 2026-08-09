import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { EventService } from '../../../services/event/event.service';
import { VenueService } from '../../../services/venue/venue.service';
import { EventCategory, EventCreateRequest, EventUpdateRequest } from '../../../models/event.model';
import { VenueResponse } from '../../../models/venue.model';
import { ApiError } from '../../../models/api-error.model';

interface EventFormValue {
  title: string;
  description: string;
  category: EventCategory;
  eventDate: string;
  eventTime: string;
  venueId: string;
}

@Component({
  selector: 'app-event-form',
  standalone: true,
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './event-form.component.html',
  styleUrl: './event-form.component.css'
})
export class EventFormComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly venueService = inject(VenueService);

  readonly isEditMode = signal(false);
  readonly eventId = signal<string | null>(null);
  readonly approvedVenues = signal<VenueResponse[]>([]);
  readonly loadingVenues = signal(true);

  readonly categories: EventCategory[] = ['MUSIC', 'SPORTS', 'CONFERENCE', 'THEATRE', 'OTHER'];

  private readonly eventModel = signal<EventFormValue>({
    title: '',
    description: '',
    category: 'MUSIC',
    eventDate: '',
    eventTime: '19:00',
    venueId: ''
  });

  readonly eventForm = form(
    this.eventModel,
    (path) => {
      required(path.title, { message: 'Title is required' });
      required(path.category, { message: 'Category is required' });
      required(path.eventDate, { message: 'Event date is required' });
      required(path.eventTime, { message: 'Event time is required' });
      required(path.venueId, { message: 'Select an approved venue' });
    },
    {
      submission: {
        action: async (field) => {
          try {
            const val = field().value();
            if (this.isEditMode() && this.eventId()) {
              const req: EventUpdateRequest = {
                title: val.title,
                description: val.description.trim() || undefined,
                category: val.category,
                eventDate: val.eventDate,
                eventTime: val.eventTime,
                venueId: val.venueId
              };
              const res = await firstValueFrom(this.eventService.update(this.eventId()!, req));
              await this.router.navigate(['/events', res.id]);
            } else {
              const req: EventCreateRequest = {
                title: val.title,
                description: val.description.trim() || undefined,
                category: val.category,
                eventDate: val.eventDate,
                eventTime: val.eventTime,
                venueId: val.venueId
              };
              const res = await firstValueFrom(this.eventService.create(req));
              await this.router.navigate(['/events', res.id]);
            }
            return;
          } catch (err) {
            const message = err instanceof ApiError ? err.message : 'Failed to save event.';
            return { kind: 'serverError', message };
          }
        }
      }
    }
  );

  constructor() {
    this.loadApprovedVenues();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.eventId.set(id);
      this.loadEventForEdit(id);
    }
  }

  readonly hasPendingVenues = signal(false);

  loadApprovedVenues(): void {
    this.venueService.listApprovedVenues().subscribe({
      next: (venues) => {
        this.approvedVenues.set(venues);
        this.loadingVenues.set(false);
      },
      error: () => this.loadingVenues.set(false)
    });

    this.venueService.listMyVenues('PENDING').subscribe({
      next: (myPending) => {
        this.hasPendingVenues.set(myPending.length > 0);
      },
      error: () => {}
    });
  }

  loadEventForEdit(id: string): void {
    this.eventService.getPublicById(id).subscribe({
      next: (evt) => {
        this.eventModel.set({
          title: evt.title,
          description: evt.description || '',
          category: evt.category,
          eventDate: evt.eventDate,
          eventTime: evt.eventTime,
          venueId: evt.venue.id
        });
      },
      error: () => {}
    });
  }
}
