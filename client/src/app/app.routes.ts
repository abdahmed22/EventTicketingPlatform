import { Routes } from '@angular/router';
import { guestGuard } from './guards/guest.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./components/auth/login/login.component').then((m) => m.LoginComponent)
  }
];