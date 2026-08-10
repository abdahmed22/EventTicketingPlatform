import { Routes } from '@angular/router';
import { guestGuard } from './guards/guest.guard';
import { adminGuard, customerGuard, organizerGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/events/event-list/event-list.component').then((m) => m.EventListComponent)
  },
  {
    path: 'events/:id',
    loadComponent: () =>
      import('./components/events/event-detail/event-detail.component').then((m) => m.EventDetailComponent)
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./components/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./components/auth/register/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'register/organizer',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./components/auth/organizer-application/organizer-application.component').then(
        (m) => m.OrganizerApplicationComponent
      )
  },
  {
    path: 'organizer/events',
    canActivate: [organizerGuard],
    loadComponent: () =>
      import('./components/organizer/my-events/my-events.component').then((m) => m.MyEventsComponent)
  },
  {
    path: 'organizer/events/new',
    canActivate: [organizerGuard],
    loadComponent: () =>
      import('./components/organizer/event-form/event-form.component').then((m) => m.EventFormComponent)
  },
  {
    path: 'organizer/events/:id/edit',
    canActivate: [organizerGuard],
    loadComponent: () =>
      import('./components/organizer/event-form/event-form.component').then((m) => m.EventFormComponent)
  },
  {
    path: 'organizer/venues',
    canActivate: [organizerGuard],
    loadComponent: () =>
      import('./components/organizer/my-venues/my-venues.component').then((m) => m.MyVenuesComponent)
  },
  {
    path: 'organizer/tickets',
    canActivate: [organizerGuard],
    loadComponent: () =>
      import('./components/organizer/event-tickets/event-tickets.component').then((m) => m.EventTicketsComponent)
  },
  {
    path: 'customer/tickets',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./components/customer/my-tickets/my-tickets.component').then((m) => m.MyTicketsComponent)
  },
  {
    path: 'customer/tickets/:ticketCode',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./components/customer/ticket-detail/ticket-detail.component').then((m) => m.TicketDetailComponent)
  },
  {
    path: 'customer/bookings',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./components/customer/my-bookings/my-bookings.component').then((m) => m.MyBookingsComponent)
  },
  {
    path: 'customer/bookings/:id',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./components/customer/booking-detail/booking-detail.component').then((m) => m.BookingDetailComponent)
  },
  {
    path: 'admin/dashboard',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./components/admin/dashboard/dashboard.component').then((m) => m.AdminDashboardComponent)
  },
  {
    path: 'admin/events',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./components/admin/admin-events/admin-events.component').then((m) => m.AdminEventsComponent)
  },
  {
    path: 'admin/tickets',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./components/admin/admin-tickets/admin-tickets.component').then((m) => m.AdminTicketsComponent)
  },
  {
    path: 'admin/organizer-applications',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./components/admin/organizer-applications-review/organizer-applications-review.component').then(
        (m) => m.OrganizerApplicationsReviewComponent
      )
  },
  {
    path: 'admin/venues',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./components/admin/venue-review/venue-review.component').then((m) => m.VenueReviewComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];