import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CsvUploadComponent } from './csv-upload.component';
import { BatchService } from '../../../core/services/batch.service';
import { ValidateCsvResponse } from '../../../core/models/validation.model';
import { BatchSummary } from '../../../core/models/batch.model';

describe('CsvUploadComponent', () => {
  let component: CsvUploadComponent;
  let fixture: ComponentFixture<CsvUploadComponent>;
  let mockBatchService: {
    validateCsv: ReturnType<typeof vi.fn>;
    uploadBatch: ReturnType<typeof vi.fn>;
  };

  const mockValidationResponse: ValidateCsvResponse = {
    validFile: true,
    fileName: 'test.csv',
    totalRowsDetected: 10,
    validRowCount: 9,
    invalidRowCount: 1,
    duplicateWithinFileCount: 0,
    detectedHeaders: ['feedback_id', 'feedback_text', 'channel', 'rating', 'feedback_date'],
    requiredHeadersFound: ['feedback_id', 'feedback_text', 'channel', 'rating', 'feedback_date'],
    missingHeaders: [],
    previewRows: [],
    validationErrors: [],
    proposedMapping: {}
  };

  const mockBatchSummary: BatchSummary = {
    batchId: 'batch-test-123',
    fileName: 'test.csv',
    sourceType: 'CSV',
    status: 'COMPLETED',
    totalRows: 10,
    validRows: 9,
    successfulRows: 9,
    failedRows: 1,
    duplicateRows: 0,
    uploadedAt: '2026-11-02T10:00:00Z',
    fileHash: 'sha-mock'
  };

  beforeEach(async () => {
    mockBatchService = {
      validateCsv: vi.fn(),
      uploadBatch: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [CsvUploadComponent],
      providers: [
        provideRouter([]),
        { provide: BatchService, useValue: mockBatchService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CsvUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create CsvUploadComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should reject non-CSV files', () => {
    const invalidFile = new File(['text'], 'report.pdf', { type: 'application/pdf' });
    const event = { target: { files: [invalidFile] } } as unknown as Event;

    component.onFileSelected(event);

    expect(component.selectedFile).toBeNull();
    expect(component.errorMessage).toContain('Please select a valid CSV file');
  });

  it('should reject empty files', () => {
    const emptyFile = new File([], 'empty.csv', { type: 'text/csv' });
    const event = { target: { files: [emptyFile] } } as unknown as Event;

    component.onFileSelected(event);

    expect(component.selectedFile).toBeNull();
    expect(component.errorMessage).toContain('Selected file is empty');
  });

  it('should accept valid CSV file and format file size', () => {
    const validFile = new File(['header1,header2\na,b'], 'valid.csv', { type: 'text/csv' });
    const event = { target: { files: [validFile] } } as unknown as Event;

    component.onFileSelected(event);

    expect(component.selectedFile).toEqual(validFile);
    expect(component.errorMessage).toBe('');
    expect(component.fileSizeFormatted).toContain('Bytes');
  });

  it('should validate CSV file successfully', () => {
    const validFile = new File(['data'], 'valid.csv', { type: 'text/csv' });
    component.selectedFile = validFile;
    mockBatchService.validateCsv.mockReturnValue(of(mockValidationResponse));

    component.validateFile();

    expect(mockBatchService.validateCsv).toHaveBeenCalledWith(validFile);
    expect(component.validationResult).toEqual(mockValidationResponse);
    expect(component.isValidating).toBe(false);
    expect(component.canUpload).toBe(true);
  });

  it('should handle validation failure', () => {
    const validFile = new File(['data'], 'valid.csv', { type: 'text/csv' });
    component.selectedFile = validFile;
    mockBatchService.validateCsv.mockReturnValue(throwError(() => ({
      error: { message: 'Malformed CSV structure' }
    })));

    component.validateFile();

    expect(component.validationResult).toBeNull();
    expect(component.errorMessage).toBe('Malformed CSV structure');
    expect(component.isValidating).toBe(false);
  });

  it('should upload batch successfully', () => {
    const validFile = new File(['data'], 'valid.csv', { type: 'text/csv' });
    component.selectedFile = validFile;
    component.validationResult = mockValidationResponse;
    mockBatchService.uploadBatch.mockReturnValue(of(mockBatchSummary));

    component.uploadFile();

    expect(mockBatchService.uploadBatch).toHaveBeenCalledWith(validFile);
    expect(component.uploadResult).toEqual(mockBatchSummary);
    expect(component.isUploading).toBe(false);
  });

  it('should handle upload error', () => {
    const validFile = new File(['data'], 'valid.csv', { type: 'text/csv' });
    component.selectedFile = validFile;
    component.validationResult = mockValidationResponse;
    mockBatchService.uploadBatch.mockReturnValue(throwError(() => ({
      error: { message: 'Database connection failed' }
    })));

    component.uploadFile();

    expect(component.uploadResult).toBeNull();
    expect(component.errorMessage).toBe('Database connection failed');
    expect(component.isUploading).toBe(false);
  });

  it('reset() should clear all state', () => {
    component.selectedFile = new File(['data'], 'valid.csv', { type: 'text/csv' });
    component.validationResult = mockValidationResponse;
    component.uploadResult = mockBatchSummary;
    component.errorMessage = 'Some error';

    component.reset();

    expect(component.selectedFile).toBeNull();
    expect(component.validationResult).toBeNull();
    expect(component.uploadResult).toBeNull();
    expect(component.errorMessage).toBe('');
  });

  it('should handle drag and drop events', () => {
    const dragEvent = {
      preventDefault: vi.fn(),
      stopPropagation: vi.fn()
    } as unknown as DragEvent;

    component.onDragOver(dragEvent);
    expect(component.isDragging).toBe(true);

    component.onDragLeave(dragEvent);
    expect(component.isDragging).toBe(false);
  });
});
