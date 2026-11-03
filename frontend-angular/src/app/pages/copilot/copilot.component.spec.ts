import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { of } from 'rxjs';
import { CopilotService } from '../../core/services/copilot.service';
import { CopilotComponent } from './copilot.component';

describe('CopilotComponent', () => {
  let fixture: ComponentFixture<CopilotComponent>;
  let component: CopilotComponent;
  let service: { query: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = {
      query: vi.fn().mockReturnValue(of({
        answer: 'Customers report duplicate charges [feedback-1].',
        agentsUsed: ['Supervisor Agent', 'Retrieval Agent', 'Reporting Agent'],
        supportingEvidence: [{
          evidenceId: 'feedback-1',
          source: 'feedback',
          title: 'Similar feedback',
          content: 'Card charged twice.',
          metadata: { similarity: 0.94 }
        }]
      }))
    };

    await TestBed.configureTestingModule({
      imports: [CopilotComponent],
      providers: [{ provide: CopilotService, useValue: service }]
    }).compileComponents();
    fixture = TestBed.createComponent(CopilotComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should submit a question and render answer, agents, and evidence', () => {
    component.query = '  What issues are customers reporting?  ';
    component.ask();
    fixture.detectChanges();

    expect(service.query).toHaveBeenCalledWith({ query: 'What issues are customers reporting?' });
    expect(fixture.nativeElement.textContent).toContain('Customers report duplicate charges');
    expect(fixture.nativeElement.textContent).toContain('Retrieval Agent');
    expect(fixture.nativeElement.textContent).toContain('Card charged twice.');
  });

  it('should reject a short question without calling the service', () => {
    component.query = ' ';
    component.ask();

    expect(component.errorMessage).toContain('at least two characters');
    expect(service.query).not.toHaveBeenCalled();
  });

  it('should display an error when the query fails', () => {
    service.query.mockReturnValue({
      subscribe: ({ error }: { error: (error: unknown) => void }) =>
        error({ error: { message: 'AI service unavailable.' } })
    });
    component.query = 'Summarize the feedback';
    component.ask();
    fixture.detectChanges();

    expect(component.errorMessage).toBe('AI service unavailable.');
    expect(component.isQuerying).toBe(false);
  });
});
