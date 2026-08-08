import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { VenueCreateRequest, VenueResponse, VenueStatus, VenueUpdateRequest } from '../../models/venue.model';

@Injectable({ providedIn: 'root' })
export class VenueService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  // Public / Organizer: List all approved venues
  listApprovedVenues(): Observable<VenueResponse[]> {
    return this.http.get<VenueResponse[]>(`${this.apiUrl}/events/venues`);
  }

  // Organizer: Submit venue request
  submit(request: VenueCreateRequest): Observable<VenueResponse> {
    return this.http.post<VenueResponse>(`${this.apiUrl}/organizer/venues`, request);
  }

  // Organizer: List own venues
  listMyVenues(status?: VenueStatus): Observable<VenueResponse[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<VenueResponse[]>(`${this.apiUrl}/organizer/venues`, { params });
  }

  // Admin: List all / pending venues
  listAdminVenues(status?: VenueStatus): Observable<VenueResponse[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<VenueResponse[]>(`${this.apiUrl}/admin/venues`, { params });
  }

  // Admin: Create auto-APPROVED venue
  adminCreate(request: VenueCreateRequest): Observable<VenueResponse> {
    return this.http.post<VenueResponse>(`${this.apiUrl}/admin/venues`, request);
  }

  // Admin: Update venue
  adminUpdate(id: string, request: VenueUpdateRequest): Observable<VenueResponse> {
    return this.http.put<VenueResponse>(`${this.apiUrl}/admin/venues/${id}`, request);
  }

  // Admin: Delete venue
  adminDelete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/venues/${id}`);
  }

  // Admin: Approve venue
  approve(id: string): Observable<VenueResponse> {
    return this.http.post<VenueResponse>(`${this.apiUrl}/admin/venues/${id}/approve`, {});
  }

  // Admin: Reject venue
  reject(id: string): Observable<VenueResponse> {
    return this.http.post<VenueResponse>(`${this.apiUrl}/admin/venues/${id}/reject`, {});
  }
}
