import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrganizerApplicationComponent } from './organizer-application.component';

describe('OrganizerApplicationComponent', () => {
  let component: OrganizerApplicationComponent;
  let fixture: ComponentFixture<OrganizerApplicationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrganizerApplicationComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(OrganizerApplicationComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
