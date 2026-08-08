import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EventResponse } from '../../models/event.model';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getPublicById(eventId: string): Observable<EventResponse> {
    return this.http.get<EventResponse>(
      `${this.apiUrl}/events/${eventId}`
    );
  }
}