export type UserRole = 'CUSTOMER' | 'ORGANIZER' | 'ADMIN';

export interface UserSummary {
  id: string;
  name: string;
  email: string;
  phone: string | null;
  role: UserRole;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  phone?: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: UserSummary;
}