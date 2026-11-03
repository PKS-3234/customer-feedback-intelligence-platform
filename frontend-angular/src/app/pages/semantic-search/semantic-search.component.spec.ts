import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { of } from 'rxjs';
import { AnalysisService } from '../../core/services/analysis.service';
import { SemanticSearchComponent } from './semantic-search.component';

describe('SemanticSearchComponent', () => {
  let fixture: ComponentFixture<SemanticSearchComponent>;
  let component: SemanticSearchComponent;
  let service: {
    semanticSearch: ReturnType<typeof vi.fn>;
    embedBatch: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    service = {
      semanticSearch: vi.fn().mockReturnValue(of([{
        feedbackId: 'feedback-1',
        batchId: 'batch-1',
        feedbackText: 'A duplicate card charge.',
        sentiment: 'NEGATIVE',
        category: 'Payments',
        theme: 'Duplicate Debit',
        emotion: 'Frustrated',
        priority: 'HIGH',
        summary: 'Card charged twice.',
        similarity: 0.94
      }])),
      embedBatch: vi.fn().mockReturnValue(of({
        batchId: 'batch-1',
        processed: 2,
        successful: 2,
        failed: 0,
        durationSeconds: 1
      }))
    };

    await TestBed.configureTestingModule({
      imports: [SemanticSearchComponent],
      providers: [{ provide: AnalysisService, useValue: service }]
    }).compileComponents();
    fixture = TestBed.createComponent(SemanticSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should search and render similar feedback', () => {
    component.query = 'duplicate card transaction';
    component.search();
    fixture.detectChanges();
    expect(service.semanticSearch).toHaveBeenCalledWith('duplicate card transaction', 10);
    expect(fixture.nativeElement.textContent).toContain('A duplicate card charge.');
    expect(fixture.nativeElement.textContent).toContain('94% similar');
  });

  it('should reject a short search query', () => {
    component.query = ' ';
    component.search();
    expect(component.errorMessage).toContain('at least two characters');
    expect(service.semanticSearch).not.toHaveBeenCalled();
  });

  it('should generate embeddings for an entered batch', () => {
    component.batchId = 'batch-1';
    component.embeddingLimit = 4;
    component.generateBatchEmbeddings();
    expect(service.embedBatch).toHaveBeenCalledWith('batch-1', 4);
    expect(component.embeddingSummary?.successful).toBe(2);
  });
});
