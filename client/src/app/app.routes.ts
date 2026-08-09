import { Routes } from '@angular/router';
import { guestGuard } from './guards/guest.guard';
import { adminGuard, customerGuard } from './guards/auth.guard';

export const routes: Routes = [
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
    path: 'admin/organizer-applications',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./components/admin/organizer-applications-review/organizer-applications-review.component').then(
        (m) => m.OrganizerApplicationsReviewComponent
      )
  },
  {

    path: 'events/:id',
    loadComponent: () =>
      import('./components/events/event-detail/event-detail.component').then(
        (m) => m.EventDetailComponent
      )
  },
  {
    path: 'bookings',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./components/booking/my-bookings/my-bookings.component').then(
        (m) => m.MyBookingsComponent
      )
  }
];