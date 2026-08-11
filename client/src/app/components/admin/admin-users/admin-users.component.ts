import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { UserAdminService } from '../../../services/user-admin/user-admin.service';
import { UserResponse } from '../../../models/user.model';
import { UserRole } from '../../../models/auth.model';
import { ApiError } from '../../../models/api-error.model';

type RoleFilter = UserRole | 'ALL';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.css'
})
export class AdminUsersComponent {
  private readonly userService = inject(UserAdminService);

  readonly roleOptions: RoleFilter[] = ['ALL', 'CUSTOMER', 'ORGANIZER', 'ADMIN'];
  readonly roleFilter = signal<RoleFilter>('ALL');

  readonly users = signal<UserResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  constructor() {
    this.load();
  }

  setFilter(filter: RoleFilter): void {
    this.roleFilter.set(filter);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);

    const role = this.roleFilter() === 'ALL' ? undefined : (this.roleFilter() as UserRole);
    this.userService.listUsers(role).subscribe({
      next: (list) => {
        this.users.set(list);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load users.');
        this.loading.set(false);
      }
    });
  }

  promoteToOrganizer(user: UserResponse): void {
    if (!confirm(`Promote "${user.name}" to ORGANIZER?`)) return;
    this.userService.changeRole(user.id, 'ORGANIZER').subscribe({
      next: () => {
        this.success.set(`${user.name} is now an ORGANIZER.`);
        this.load();
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to change role.');
        this.loading.set(false);
      }
    });
  }

  demoteToCustomer(user: UserResponse): void {
    if (!confirm(`Demote "${user.name}" to CUSTOMER?`)) return;
    this.userService.changeRole(user.id, 'CUSTOMER').subscribe({
      next: () => {
        this.success.set(`${user.name} is now a CUSTOMER.`);
        this.load();
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to change role.');
        this.loading.set(false);
      }
    });
  }

  deleteUser(user: UserResponse): void {
    if (!confirm(`Delete user "${user.name}" (${user.email})? This cannot be undone.`)) return;
    this.userService.deleteUser(user.id).subscribe({
      next: () => {
        this.success.set(`User "${user.name}" was deleted.`);
        this.load();
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to delete user.');
        this.loading.set(false);
      }
    });
  }
}
