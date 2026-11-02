import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { BatchDetailComponent } from './batch-detail.component';
import { BatchService } from '../../../core/services/batch.service';
import { BatchDetail } from '../../../core/models/batch.model';
import { ProcessingError } from '../../../core/models/error.model';
import { PagedResponse } from '../../../core/models/pagination.model';

describe('BatchDetailComponent', () => {
  let component: BatchDetailComponent;
  let fixture: ComponentFixture<BatchDetailComponent>;
  let mockBatchService: {
    getBatchDetail: ReturnType<typeof vi.fn>;
    getBatchErrors: ReturnType<typeof vi.fn>;
  };

  const mockBatchDetail: BatchDetail = {
    batchId: 'test-batch-uuid',
    fileName: 'banking_feedback.csv',
    fileHash: 'sha256-abcdef',
    sourceType: 'CSV',
    uploadedBy: 'operator',
    status: 'COMPLETED',
    totalRows: 100,
    validRows: 95,
    successfulRows: 95,
    failedRows: 5,
    duplicateRows: 0,
    uploadedAt: '2026-11-02T10:00:00Z',
    createdAt: '2026-11-02T10:00:00Z',
    updatedAt: '2026-11-02T10:01:00Z'
  };

  const mockErrors: ProcessingError[] = [
    {
      errorId: 'err-1',
      batchId: 'test-batch-uuid',
      csvRowNumber: 14,
      errorCode: 'INVALID_RATING',
      errorMessage: 'Rating 8 is out of range 1-5',
      processingStage: 'ROW_VALIDATION',
      originalRow: 'F0014,web,8',
      createdAt: '2026-11-02T10:00:01Z',
      resolved: false
    }
  ];

  const mockErrorsPaged: PagedResponse<ProcessingError> = {
    content: mockErrors,
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    last: true
  };

  beforeEach(async () => {
    mockBatchService = {
      getBatchDetail: vi.fn().mockReturnValue(of(mockBatchDetail)),
      getBatchErrors: vi.fn().mockReturnValue(of(mockErrorsPaged))
    };

    await TestBed.configureTestingModule({
      imports: [BatchDetailComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'batchId' ? 'test-batch-uuid' : null)
              }
            }
          }
        },
        { provide: BatchService, useValue: mockBatchService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BatchDetailComponent);
    component = fixture.componentInstance;
  });

  it('should create BatchDetailComponent and load details', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(mockBatchService.getBatchDetail).toHaveBeenCalledWith('test-batch-uuid');
    expect(mockBatchService.getBatchErrors).toHaveBeenCalledWith('test-batch-uuid', 0, 20, undefined);
    expect(component.batch).toEqual(mockBatchDetail);
    expect(component.errors.length).toBe(1);
    expect(component.isLoadingBatch).toBe(false);
  });

  it('should filter errors by stage', () => {
    fixture.detectChanges();

    const event = { target: { value: 'ROW_VALIDATION' } } as unknown as Event;
    component.onStageFilterChange(event);

    expect(component.selectedStage).toBe('ROW_VALIDATION');
    expect(mockBatchService.getBatchErrors).toHaveBeenCalledWith('test-batch-uuid', 0, 20, 'ROW_VALIDATION');
  });

  it('should toggle expanded error row', () => {
    expect(component.expandedErrorRow).toBeNull();

    component.toggleExpandRow('err-1');
    expect(component.expandedErrorRow).toBe('err-1');

    component.toggleExpandRow('err-1');
    expect(component.expandedErrorRow).toBeNull();
  });

  it('should handle batch loading error', () => {
    mockBatchService.getBatchDetail.mockReturnValue(throwError(() => ({
      error: { message: 'Batch not found' }
    })));

    component.loadBatch();

    expect(component.isLoadingBatch).toBe(false);
    expect(component.errorMessage).toBe('Batch not found');
  });
});
