import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AnalysisComponent } from './analysis.component';
import { AnalysisService } from '../../core/services/analysis.service';

describe('AnalysisComponent', () => {
  let fixture: ComponentFixture<AnalysisComponent>;
  let component: AnalysisComponent;
  let analysisService: {
    analyzeBatch: ReturnType<typeof vi.fn>;
    getBatchAnalysis: ReturnType<typeof vi.fn>;
    analyzeFeedback: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    analysisService = {
      analyzeBatch: vi.fn().mockReturnValue(of({
        batchId: 'batch-1',
        processed: 2,
        successful: 2,
        failed: 0,
        durationSeconds: 1
      })),
      getBatchAnalysis: vi.fn().mockReturnValue(of([{
        analysisId: 'analysis-1',
        feedbackId: 'feedback-1',
        sentiment: 'POSITIVE',
        category: 'Support',
        theme: 'Helpful',
        emotion: 'Satisfied',
        priority: 'LOW',
        assignedDepartment: 'Support Team',
        summary: 'Helpful service.',
        confidence: 0.95,
        analyzedAt: '2026-11-03T10:00:00Z',
        modelName: 'gemini-2.5-flash',
        promptVersion: 'v1.0',
        isCurrent: true,
        createdAt: '2026-11-03T10:00:00Z'
      }])),
      analyzeFeedback: vi.fn().mockReturnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [AnalysisComponent],
      providers: [{ provide: AnalysisService, useValue: analysisService }]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalysisComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should run batch analysis and render persisted results', () => {
    component.batchId = 'batch-1';
    component.runBatchAnalysis();
    fixture.detectChanges();

    expect(analysisService.analyzeBatch).toHaveBeenCalledWith('batch-1', undefined);
    expect(analysisService.getBatchAnalysis).toHaveBeenCalledWith('batch-1');
    expect(component.summary?.successful).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Helpful service.');
    expect(fixture.nativeElement.textContent).toContain('POSITIVE');
  });

  it('should require a batch ID before starting', () => {
    component.runBatchAnalysis();
    expect(component.errorMessage).toContain('Enter a batch ID');
    expect(analysisService.analyzeBatch).not.toHaveBeenCalled();
  });

  it('should display an error when the batch request fails', () => {
    analysisService.analyzeBatch.mockReturnValue(throwError(() => ({
      error: { message: 'AI service unavailable' }
    })));
    component.batchId = 'batch-1';
    component.runBatchAnalysis();
    fixture.detectChanges();
    expect(component.errorMessage).toBe('AI service unavailable');
  });
});
