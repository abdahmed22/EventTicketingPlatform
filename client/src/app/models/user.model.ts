import { UserRole } from './auth.model';

export interface UserResponse {
  id: string;
  name: string;
  email: string;
  phone: string | null;
  role: UserRole;
  createdAt: string;
}

export interface ChangeRoleRequest {
  role: UserRole;
}
