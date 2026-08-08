import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrganizerApplicationsReviewComponent } from './organizer-applications-review.component';

describe('OrganizerApplicationsReviewComponent', () => {
  let component: OrganizerApplicationsReviewComponent;
  let fixture: ComponentFixture<OrganizerApplicationsReviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrganizerApplicationsReviewComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(OrganizerApplicationsReviewComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
