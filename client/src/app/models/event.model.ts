import { SeatCategorySummary } from './seat-category.model';

export type EventCategory =
  | 'CONCERT'
  | 'CONFERENCE'
  | 'SPORTS'
  | 'THEATER'
  | 'OTHER';

export type EventStatus =
  | 'DRAFT'
  | 'PUBLISHED'
  | 'CANCELLED'
  | 'COMPLETED';

export interface EventResponse {
  id: string;
  title: string;
  description: string;
  category: EventCategory;
  eventDate: string;
  eventTime: string;
  status: EventStatus;
  seatCategories: SeatCategorySummary[];
  organizerId: string;
}