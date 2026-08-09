import { VenueSummary } from './venue.model';
import { SeatCategorySummary } from './seat-category.model';

export type EventCategory = 'MUSIC' | 'SPORTS' | 'CONFERENCE' | 'THEATRE' | 'OTHER';
export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED';

export interface EventSummary {
  id: string;
  title: string;
  description?: string;
  category: EventCategory;
  eventDate: string;
  eventTime: string;
  status: EventStatus;
  venueId: string;
  organizerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface EventResponse {
  id: string;
  title: string;
  description?: string;
  category: EventCategory;
  eventDate: string;
  eventTime: string;
  status: EventStatus;
  venue: VenueSummary;
  organizerId: string;
  seatCategories: SeatCategorySummary[];
  createdAt: string;
  updatedAt: string;
}

export interface EventCreateRequest {
  title: string;
  description?: string;
  category: EventCategory;
  eventDate: string;
  eventTime: string;
  venueId: string;
}

export interface EventUpdateRequest {
  title?: string;
  description?: string;
  category?: EventCategory;
  eventDate?: string;
  eventTime?: string;
  venueId?: string;
}

export interface EventFilterParams {
  category?: EventCategory;
  dateFrom?: string;
  dateTo?: string;
  minPrice?: number;
  maxPrice?: number;
  venueId?: string;
  organizerId?: string;
  status?: EventStatus;
  page?: number;
  size?: number;
}
