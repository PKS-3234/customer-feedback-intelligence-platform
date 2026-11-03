import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AnalysisService } from './analysis.service';
import { environment } from '../../../environments/environment';

describe('AnalysisService', () => {
  let service: AnalysisService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AnalysisService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should request batch analysis with an optional limit', () => {
    service.analyzeBatch('batch-123', 10).subscribe();

    const request = httpMock.expectOne(
      `${environment.apiBaseUrl}/analysis/batches/batch-123?limit=10`
    );
    expect(request.request.method).toBe('POST');
    request.flush({
      batchId: 'batch-123',
      processed: 10,
      successful: 9,
      failed: 1,
      durationSeconds: 2
    });
  });

  it('should retrieve analysis results for a batch', () => {
    service.getBatchAnalysis('batch-123').subscribe(results => {
      expect(results.length).toBe(1);
      expect(results[0].sentiment).toBe('NEGATIVE');
    });

    const request = httpMock.expectOne(
      `${environment.apiBaseUrl}/analysis/batches/batch-123`
    );
    expect(request.request.method).toBe('GET');
    request.flush([{
      analysisId: 'analysis-1',
      feedbackId: 'feedback-1',
      sentiment: 'NEGATIVE',
      category: 'Payments',
      theme: 'Refund Delay',
      emotion: 'Frustrated',
      priority: 'HIGH',
      assignedDepartment: 'Payments Team',
      summary: 'Refund is delayed.',
      confidence: 0.9,
      analyzedAt: '2026-11-03T10:00:00Z',
      modelName: 'gemini-2.5-flash',
      promptVersion: 'v1.0',
      isCurrent: true,
      createdAt: '2026-11-03T10:00:00Z'
    }]);
  });

  it('should request semantic search with a query and result limit', () => {
    service.semanticSearch('card charged twice', 5).subscribe();

    const request = httpMock.expectOne(`${environment.apiBaseUrl}/semantic-search`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ query: 'card charged twice', limit: 5 });
    request.flush([]);
  });

  it('should trigger batch embedding generation', () => {
    service.embedBatch('batch-123', 3).subscribe();

    const request = httpMock.expectOne(
      `${environment.apiBaseUrl}/embeddings/batches/batch-123?limit=3`
    );
    expect(request.request.method).toBe('POST');
    request.flush({
      batchId: 'batch-123',
      processed: 3,
      successful: 3,
      failed: 0,
      durationSeconds: 1
    });
  });
});
