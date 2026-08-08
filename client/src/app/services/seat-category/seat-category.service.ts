import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SeatCategorySummary } from '../../models/seat-category.model';

@Injectable({ providedIn: 'root' })
export class SeatCategoryService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  listByEvent(eventId: string): Observable<SeatCategorySummary[]> {
    return this.http.get<SeatCategorySummary[]>(
      `${this.apiUrl}/events/${eventId}/seat-categories`
    );
  }
}