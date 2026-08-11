import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse, ChangeRoleRequest } from '../../models/user.model';
import { UserRole } from '../../models/auth.model';

@Injectable({ providedIn: 'root' })
export class UserAdminService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /** GET /api/admin/users — optionally filtered by role */
  listUsers(role?: UserRole): Observable<UserResponse[]> {
    let params = new HttpParams();
    if (role) params = params.set('role', role);
    return this.http.get<UserResponse[]>(`${this.apiUrl}/admin/users`, { params });
  }

  /** GET /api/admin/users/{id} */
  getUser(id: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/admin/users/${id}`);
  }

  /** PATCH /api/admin/users/{id}/role */
  changeRole(id: string, role: UserRole): Observable<UserResponse> {
    const body: ChangeRoleRequest = { role };
    return this.http.patch<UserResponse>(`${this.apiUrl}/admin/users/${id}/role`, body);
  }

  /** DELETE /api/admin/users/{id} */
  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/users/${id}`);
  }
}
