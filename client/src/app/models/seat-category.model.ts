export interface SeatCategorySummary {
  id: string;
  name: string;
  price: number;
  totalSeats: number;
  availableSeats: number;
  seatingCapacity: number;
}

export interface SeatCategoryResponse {
  id: string;
  eventId: string;
  venueId: string;
  name: string;
  price: number;
  totalSeats: number;
  availableSeats: number;
  seatingCapacity: number;
}

export interface SeatCategoryCreateRequest {
  name: string;
  price: number;
  totalSeats: number;
  seatingCapacity: number;
}

export interface SeatCategoryUpdateRequest {
  name?: string;
  price?: number;
  totalSeats?: number;
  seatingCapacity?: number;
}
