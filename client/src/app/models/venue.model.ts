export type VenueStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface VenueSummary {
  id: string;
  name: string;
  address: string;
  capacity: number;
}

export interface VenueResponse {
  id: string;
  name: string;
  address: string;
  capacity: number;
  status: VenueStatus;
  reviewedAt?: string;
  reviewedBy?: string;
  requestedBy?: string;
}

export interface VenueCreateRequest {
  name: string;
  address: string;
  capacity: number;
}

export interface VenueUpdateRequest {
  name: string;
  address: string;
  capacity: number;
}
