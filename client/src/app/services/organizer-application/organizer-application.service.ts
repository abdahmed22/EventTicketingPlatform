import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OrganizerApplicationRequest, OrganizerApplicationResponse, OrganizerApplicationStatus } from '../../models/organizer-application.model';
import { UserSummary } from '../../models/auth.model';

@Injectable({ providedIn: 'root' })
export class OrganizerApplicationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  submit(request: OrganizerApplicationRequest): Observable<OrganizerApplicationResponse> {
    return this.http.post<OrganizerApplicationResponse>(`${this.apiUrl}/register/organizer-application`, request);
  }

  list(status?: OrganizerApplicationStatus): Observable<OrganizerApplicationResponse[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<OrganizerApplicationResponse[]>(`${this.apiUrl}/admin/organizer-applications`, { params });
  }

  approve(id: string): Observable<UserSummary> {
    return this.http.post<UserSummary>(`${this.apiUrl}/admin/organizer-applications/${id}/approve`, {});
  }

  reject(id: string, rejectionReason?: string): Observable<OrganizerApplicationResponse> {
    return this.http.post<OrganizerApplicationResponse>(
      `${this.apiUrl}/admin/organizer-applications/${id}/reject`,
      { rejectionReason }
    );
  }
}