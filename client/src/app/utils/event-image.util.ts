import { EventCategory } from '../models/event.model';

export const CATEGORY_IMAGE_MAP: Record<EventCategory, string> = {
  MUSIC: '/images/event-categories/music.jpg',
  SPORTS: '/images/event-categories/sports.jpg',
  CONFERENCE: '/images/event-categories/conference.jpg',
  THEATRE: '/images/event-categories/theatre.jpg',
  OTHER: '/images/event-categories/other.jpg'
};

/**
 * Returns the public image path for a given Event Category.
 * Falls back to 'OTHER' (/images/event-categories/other.jpg) if category is unknown or missing.
 */
export function getEventCategoryImage(category?: EventCategory | string | null): string {
  if (!category) {
    return CATEGORY_IMAGE_MAP.OTHER;
  }
  const upper = category.toUpperCase() as EventCategory;
  return CATEGORY_IMAGE_MAP[upper] ?? CATEGORY_IMAGE_MAP.OTHER;
}
