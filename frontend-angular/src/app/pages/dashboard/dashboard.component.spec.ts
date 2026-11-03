import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';
import { DashboardMetrics } from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardComponent } from './dashboard.component';

const metrics: DashboardMetrics = {
  totalFeedbackRecords: 100,
  totalAnalyzedRecords: 80,
  totalEmbeddedRecords: 70,
  positiveFeedbackPercent: 25,
  negativeFeedbackPercent: 60,
  highPriorityFeedbackCount: 14,
  currentBatches: 2,
  processingErrors: 5,
  unresolvedProcessingErrors: 3,
  sentimentDistribution: [
    { label: 'NEGATIVE', count: 48 },
    { label: 'POSITIVE', count: 20 }
  ],
  categoryDistribution: [{ label: 'Payments', count: 30 }],
  themeDistribution: [{ label: 'Refund delay', count: 12 }],
  priorityDistribution: [{ label: 'HIGH', count: 14 }],
  feedbackByChannel: [{ label: 'Mobile App', count: 65 }],
  feedbackByProduct: [{ label: 'Cards', count: 50 }],
  semanticSearch: {
    totalEmbeddings: 70,
    models: [{ model: 'gemini-embedding-001', dimension: 768, count: 70 }],
    vectorDimension: 768,
    status: 'Ready',
    statusMessage: 'PGVector is ready with current feedback embeddings.'
  }
};

const agents = [
  {
    agent: 'Supervisor Agent',
    purpose: 'Routes questions to the relevant agents.',
    availability: 'Configured',
    lastExecution: null
  },
  {
    agent: 'Analytics Agent',
    purpose: 'Aggregates feedback metrics.',
    availability: 'Configured',
    lastExecution: null
  },
  {
    agent: 'Retrieval Agent',
    purpose: 'Uses PGVector retrieval.',
    availability: 'Configured',
    lastExecution: null
  },
  {
    agent: 'Data Quality Agent',
    purpose: 'Checks coverage and errors.',
    availability: 'Configured',
    lastExecution: null
  },
  {
    agent: 'Reporting Agent',
    purpose: 'Grounded reporting.',
    availability: 'Configured',
    lastExecution: null
  }
];

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;
  let service: {
    getMetrics: ReturnType<typeof vi.fn>;
    getAgentStatuses: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    service = {
      getMetrics: vi.fn().mockReturnValue(of(metrics)),
      getAgentStatuses: vi.fn().mockReturnValue(of(agents))
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: DashboardService, useValue: service }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads and displays live metrics, all chart groups, and agent statuses', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(service.getMetrics).toHaveBeenCalledOnce();
    expect(service.getAgentStatuses).toHaveBeenCalledOnce();
    expect(text).toContain('Total Feedback Records');
    expect(text).toContain('100');
    expect(text).toContain('Sentiment Distribution');
    expect(text).toContain('Category Distribution');
    expect(text).toContain('Theme Distribution');
    expect(text).toContain('Priority Distribution');
    expect(text).toContain('Feedback by Channel');
    expect(text).toContain('Feedback by Product');
    expect(text).toContain('gemini-embedding-001');
    expect(text).toContain('Supervisor Agent');
    expect(text).toContain('Not tracked');
    expect(text).toContain('What are customers unhappy about?');
  });

  it('scales chart bars against the largest returned count', () => {
    expect(component.barWidth(25, [{ label: 'a', count: 50 }, { label: 'b', count: 25 }])).toBe(50);
    expect(component.barWidth(0, [])).toBe(0);
  });

  it('shows an explicit loading error when dashboard APIs fail', () => {
    service.getMetrics.mockReturnValue(
      throwError(() => ({ error: { message: 'Dashboard database unavailable.' } }))
    );

    component.refresh();
    fixture.detectChanges();

    expect(component.errorMessage).toBe('Dashboard database unavailable.');
    expect(component.isLoading).toBe(false);
  });
});
