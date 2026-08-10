import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DashboardService } from '../../../services/dashboard/dashboard.service';
import { OrganizerApplicationService } from '../../../services/organizer-application/organizer-application.service';
import { VenueService } from '../../../services/venue/venue.service';
import { DashboardSummary } from '../../../models/dashboard.model';
import { ApiError } from '../../../models/api-error.model';

type RejectKind = 'application' | 'venue';
interface RejectTarget {
  kind: RejectKind;
  id: string;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class AdminDashboardComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly organizerApplicationService = inject(OrganizerApplicationService);
  private readonly venueService = inject(VenueService);

  readonly summary = signal<DashboardSummary | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly actioningId = signal<string | null>(null);
  readonly rejecting = signal<RejectTarget | null>(null);
  readonly rejectionReason = signal('');

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);

    this.dashboardService.getPending().subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load the review dashboard.');
        this.loading.set(false);
      }
    });
  }

  // ---- Organizer applications ----

  approveApplication(id: string): void {
    this.actioningId.set(id);
    this.error.set(null);
    this.organizerApplicationService.approve(id).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.success.set('Organizer application approved. A live organizer account was created.');
        this.load();
      },
      error: (err: unknown) => {
        this.actioningId.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to approve application.');
      }
    });
  }

  startRejectApplication(id: string): void {
    this.rejecting.set({ kind: 'application', id });
    this.rejectionReason.set('');
  }

  confirmRejectApplication(id: string): void {
    this.actioningId.set(id);
    this.error.set(null);
    const reason = this.rejectionReason().trim() || undefined;

    this.organizerApplicationService.reject(id, reason).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.cancelReject();
        this.success.set('Organizer application rejected.');
        this.load();
      },
      error: (err: unknown) => {
        this.actioningId.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to reject application.');
      }
    });
  }

  // ---- Venues ----

  approveVenue(id: string): void {
    this.actioningId.set(id);
    this.error.set(null);
    this.venueService.approve(id).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.success.set('Venue approved. It is now available to all organizers.');
        this.load();
      },
      error: (err: unknown) => {
        this.actioningId.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to approve venue.');
      }
    });
  }

  startRejectVenue(id: string): void {
    this.rejecting.set({ kind: 'venue', id });
    this.rejectionReason.set('');
  }

  confirmRejectVenue(id: string): void {
    this.actioningId.set(id);
    this.error.set(null);
    const reason = this.rejectionReason().trim() || undefined;

    this.venueService.reject(id, reason).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.cancelReject();
        this.success.set('Venue request rejected.');
        this.load();
      },
      error: (err: unknown) => {
        this.actioningId.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to reject venue.');
      }
    });
  }

  // ---- Shared reject UI helpers ----

  isRejecting(kind: RejectKind, id: string): boolean {
    const target = this.rejecting();
    return target?.kind === kind && target?.id === id;
  }

  cancelReject(): void {
    this.rejecting.set(null);
    this.rejectionReason.set('');
  }
}
