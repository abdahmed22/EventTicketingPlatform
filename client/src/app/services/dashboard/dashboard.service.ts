import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardSummary } from '../../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  // Admin: load both pending review queues in one request.
  // Backend route: GET /api/admin/dashboard
  getPending(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.apiUrl}/admin/dashboard`);
  }
}
