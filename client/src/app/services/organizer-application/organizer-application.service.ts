import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OrganizerApplicationRequest, OrganizerApplicationResponse } from '../../models/organizer-application.model';

@Injectable({ providedIn: 'root' })
export class OrganizerApplicationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  submit(request: OrganizerApplicationRequest): Observable<OrganizerApplicationResponse> {
    return this.http.post<OrganizerApplicationResponse>(`${this.apiUrl}/register/organizer-application`, request);
  }
}