import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Location } from '@angular/common';
import { vi } from 'vitest';
import { of } from 'rxjs';
import { routes } from './app.routes';
import { BatchService } from './core/services/batch.service';
import { AnalysisService } from './core/services/analysis.service';

describe('App Routing', () => {
  let router: Router;
  let location: Location;
  let mockBatchService: {
    listBatches: ReturnType<typeof vi.fn>;
    getBatchDetail: ReturnType<typeof vi.fn>;
    getBatchErrors: ReturnType<typeof vi.fn>;
  };
  const mockAnalysisService = {
    analyzeBatch: vi.fn().mockReturnValue(of({
      batchId: 'test',
      processed: 0,
      successful: 0,
      failed: 0,
      durationSeconds: 0
    })),
    getBatchAnalysis: vi.fn().mockReturnValue(of([])),
    analyzeFeedback: vi.fn().mockReturnValue(of({}))
  };

  beforeEach(async () => {
    mockBatchService = {
      listBatches: vi.fn().mockReturnValue(of({
        content: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        last: true
      })),
      getBatchDetail: vi.fn().mockReturnValue(of({
        batchId: '123',
        fileName: 'test.csv',
        fileHash: 'hash',
        sourceType: 'CSV',
        uploadedBy: 'test',
        status: 'COMPLETED',
        totalRows: 0,
        validRows: 0,
        successfulRows: 0,
        failedRows: 0,
        duplicateRows: 0,
        uploadedAt: '2026-11-02T10:00:00Z',
        createdAt: '2026-11-02T10:00:00Z',
        updatedAt: '2026-11-02T10:00:00Z'
      })),
      getBatchErrors: vi.fn().mockReturnValue(of({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        last: true
      }))
    };

    await TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        { provide: BatchService, useValue: mockBatchService },
        { provide: AnalysisService, useValue: mockAnalysisService }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    location = TestBed.inject(Location);
    router.initialNavigation();
  });

  it('should navigate to empty path as Home', async () => {
    await router.navigate(['']);
    expect(location.path()).toBe('');
  });

  it('should navigate to /data-operations/upload', async () => {
    await router.navigate(['/data-operations/upload']);
    expect(location.path()).toBe('/data-operations/upload');
  });

  it('should navigate to /data-operations/batches', async () => {
    await router.navigate(['/data-operations/batches']);
    expect(location.path()).toBe('/data-operations/batches');
  });

  it('should navigate to /data-operations/batches/:batchId', async () => {
    await router.navigate(['/data-operations/batches/test-id-123']);
    expect(location.path()).toBe('/data-operations/batches/test-id-123');
  });

  it('should navigate to /analysis', async () => {
    await router.navigate(['/analysis']);
    expect(location.path()).toBe('/analysis');
  });

  it('should navigate to /semantic-search', async () => {
    await router.navigate(['/semantic-search']);
    expect(location.path()).toBe('/semantic-search');
  });

  it('should redirect unknown routes to Home', async () => {
    await router.navigate(['/non-existent-route-xyz']);
    expect(location.path()).toBe('');
  });
});
