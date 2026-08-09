import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserRole,
  UserSummary
} from '../../models/auth.model';

const TOKEN_KEY = 'eventticketing_token';
const USER_KEY = 'eventticketing_user';

function readStoredUser(): UserSummary | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as UserSummary;
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  private readonly tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private readonly userSignal = signal<UserSummary | null>(readStoredUser());

  readonly token = this.tokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly isLoggedIn = computed(() => this.token() !== null);
  readonly role = computed<UserRole | null>(() => {
    const r = this.user()?.role;
    return r ? (r.toUpperCase() as UserRole) : null;
  });
  readonly isCustomer = computed(() => this.role() === 'CUSTOMER');
  readonly isOrganizer = computed(() => this.role() === 'ORGANIZER');
  readonly isAdmin = computed(() => this.role() === 'ADMIN');

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, request)
      .pipe(tap((response) => this.persistSession(response)));
  }

  register(request: RegisterRequest): Observable<UserSummary> {
    return this.http.post<UserSummary>(`${this.apiUrl}/register`, request);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
  }

  private persistSession(response: LoginResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
    this.tokenSignal.set(response.token);
    this.userSignal.set(response.user);
  }
}