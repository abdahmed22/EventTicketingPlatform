import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  SeatCategoryCreateRequest,
  SeatCategoryResponse,
  SeatCategorySummary,
  SeatCategoryUpdateRequest
} from '../../models/seat-category.model';

@Injectable({ providedIn: 'root' })
export class SeatCategoryService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  // Public/Customer: List seat categories by event
  listByEvent(eventId: string): Observable<SeatCategorySummary[]> {
    return this.http.get<SeatCategorySummary[]>(`${this.apiUrl}/events/${eventId}/seat-categories`);
  }

  // Organizer: Create seat category for an event
  create(eventId: string, request: SeatCategoryCreateRequest): Observable<SeatCategoryResponse> {
    return this.http.post<SeatCategoryResponse>(`${this.apiUrl}/organizer/events/${eventId}/seat-categories`, request);
  }

  // Organizer: Update seat category
  update(id: string, request: SeatCategoryUpdateRequest): Observable<SeatCategoryResponse> {
    return this.http.put<SeatCategoryResponse>(`${this.apiUrl}/organizer/seat-categories/${id}`, request);
  }
}
