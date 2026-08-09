import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavHeaderComponent } from './components/layout/nav-header/nav-header.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavHeaderComponent],
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './app.css',
})
export class App {}
