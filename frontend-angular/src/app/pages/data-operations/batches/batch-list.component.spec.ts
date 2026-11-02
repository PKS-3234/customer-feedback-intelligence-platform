import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { BatchListComponent } from './batch-list.component';
import { BatchService } from '../../../core/services/batch.service';
import { BatchSummary } from '../../../core/models/batch.model';
import { PagedResponse } from '../../../core/models/pagination.model';

describe('BatchListComponent', () => {
  let component: BatchListComponent;
  let fixture: ComponentFixture<BatchListComponent>;
  let mockBatchService: {
    listBatches: ReturnType<typeof vi.fn>;
  };

  const mockBatches: BatchSummary[] = [
    {
      batchId: 'batch-001',
      fileName: 'feedback_batch_1.csv',
      sourceType: 'CSV',
      status: 'COMPLETED',
      totalRows: 100,
      validRows: 98,
      successfulRows: 98,
      failedRows: 2,
      duplicateRows: 0,
      uploadedAt: '2026-11-02T10:00:00Z',
      fileHash: 'hash001'
    },
    {
      batchId: 'batch-002',
      fileName: 'feedback_batch_2.csv',
      sourceType: 'CSV',
      status: 'COMPLETED_WITH_ERRORS',
      totalRows: 50,
      validRows: 40,
      successfulRows: 40,
      failedRows: 5,
      duplicateRows: 5,
      uploadedAt: '2026-11-02T11:00:00Z',
      fileHash: 'hash002'
    }
  ];

  const mockPagedResponse: PagedResponse<BatchSummary> = {
    content: mockBatches,
    page: 0,
    size: 10,
    totalElements: 2,
    totalPages: 1,
    last: true
  };

  beforeEach(async () => {
    mockBatchService = {
      listBatches: vi.fn().mockReturnValue(of(mockPagedResponse))
    };

    await TestBed.configureTestingModule({
      imports: [BatchListComponent],
      providers: [
        provideRouter([]),
        { provide: BatchService, useValue: mockBatchService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BatchListComponent);
    component = fixture.componentInstance;
  });

  it('should create BatchListComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should load batches on initialization', () => {
    fixture.detectChanges();

    expect(mockBatchService.listBatches).toHaveBeenCalledWith(0, 10, undefined);
    expect(component.batches.length).toBe(2);
    expect(component.totalElements).toBe(2);
    expect(component.isLoading).toBe(false);
  });

  it('should filter by status when changed', () => {
    fixture.detectChanges();

    const event = { target: { value: 'COMPLETED' } } as unknown as Event;
    component.onStatusFilterChange(event);

    expect(component.selectedStatus).toBe('COMPLETED');
    expect(mockBatchService.listBatches).toHaveBeenCalledWith(0, 10, 'COMPLETED');
  });

  it('should navigate between pages', () => {
    mockBatchService.listBatches.mockReturnValue(of({
      content: mockBatches,
      page: 1,
      size: 10,
      totalElements: 50,
      totalPages: 5,
      last: false
    }));

    component.page = 1;
    component.totalPages = 5;

    component.previousPage();
    expect(mockBatchService.listBatches).toHaveBeenCalledWith(0, 10, undefined);

    component.page = 1;
    component.totalPages = 5;
    component.nextPage();
    expect(mockBatchService.listBatches).toHaveBeenCalledWith(2, 10, undefined);
  });

  it('should handle load errors gracefully', () => {
    mockBatchService.listBatches.mockReturnValue(throwError(() => ({
      error: { message: 'Server unreachable' }
    })));

    component.loadBatches(0);

    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('Server unreachable');
  });
});
