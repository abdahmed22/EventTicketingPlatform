import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { OrganizerApplicationService } from '../../../services/organizer-application/organizer-application.service';
import { OrganizerApplicationResponse, OrganizerApplicationStatus } from '../../../models/organizer-application.model';
import { ApiError } from '../../../models/api-error.model';

type StatusFilter = OrganizerApplicationStatus | 'ALL';

@Component({
  selector: 'app-organizer-applications-review',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './organizer-applications-review.component.html',
  styleUrl: './organizer-applications-review.component.css'
})
export class OrganizerApplicationsReviewComponent {
  private readonly organizerApplicationService = inject(OrganizerApplicationService);

  readonly statusOptions: StatusFilter[] = ['ALL', 'PENDING', 'APPROVED', 'REJECTED'];

  readonly applications = signal<OrganizerApplicationResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly statusFilter = signal<StatusFilter>('PENDING');

  readonly actioningId = signal<string | null>(null);
  readonly rejectingId = signal<string | null>(null);
  readonly rejectionReason = signal('');

  constructor() {
    this.load();
  }

  setFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const status = this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as OrganizerApplicationStatus);

    this.organizerApplicationService.list(status).subscribe({
      next: (applications) => {
        this.applications.set(applications);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(err instanceof ApiError ? err.message : 'Failed to load applications.');
        this.loading.set(false);
      }
    });
  }

  approve(id: string): void {
    this.actioningId.set(id);
    this.error.set(null);

    this.organizerApplicationService.approve(id).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.load();
      },
      error: (err: unknown) => {
        this.actioningId.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to approve application.');
      }
    });
  }

  startReject(id: string): void {
    this.rejectingId.set(id);
    this.rejectionReason.set('');
  }

  cancelReject(): void {
    this.rejectingId.set(null);
    this.rejectionReason.set('');
  }

  confirmReject(id: string): void {
    this.actioningId.set(id);
    this.error.set(null);
    const reason = this.rejectionReason().trim() || undefined;

    this.organizerApplicationService.reject(id, reason).subscribe({
      next: () => {
        this.actioningId.set(null);
        this.rejectingId.set(null);
        this.rejectionReason.set('');
        this.load();
      },
      error: (err: unknown) => {
        this.actioningId.set(null);
        this.error.set(err instanceof ApiError ? err.message : 'Failed to reject application.');
      }
    });
  }
}