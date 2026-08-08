export type OrganizerApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface OrganizerApplicationRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
  organizationName?: string;
  reason?: string;
}

export interface OrganizerApplicationResponse {
  id: string;
  name: string;
  email: string;
  phone: string;
  organizationName: string | null;
  reason: string | null;
  status: OrganizerApplicationStatus;
  submittedAt: string;
  reviewedAt: string | null;
  reviewedByName: string | null;
  rejectionReason: string | null;
}