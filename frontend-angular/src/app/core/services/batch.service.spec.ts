import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { BatchService } from './batch.service';
import { environment } from '../../../environments/environment';
import { BatchSummary, BatchDetail } from '../models/batch.model';
import { ValidateCsvResponse } from '../models/validation.model';
import { PagedResponse } from '../models/pagination.model';
import { ProcessingError } from '../models/error.model';
import { Feedback } from '../models/feedback.model';

describe('BatchService', () => {
  let service: BatchService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiBaseUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BatchService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(BatchService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('validateCsv should POST to /batches/validate with FormData', () => {
    const mockFile = new File(['header1,header2\nval1,val2'], 'test.csv', { type: 'text/csv' });
    const mockResponse: ValidateCsvResponse = {
      validFile: true,
      fileName: 'test.csv',
      detectedHeaders: ['feedback_id', 'feedback_text'],
      requiredHeadersFound: ['feedback_id', 'feedback_text'],
      missingHeaders: [],
      totalRowsDetected: 1,
      validRowCount: 1,
      invalidRowCount: 0,
      duplicateWithinFileCount: 0,
      previewRows: [{ feedback_id: 'F1', feedback_text: 'Great' }],
      validationErrors: [],
      proposedMapping: { feedback_id: 'feedback_id', feedback_text: 'feedback_text' }
    };

    service.validateCsv(mockFile).subscribe(res => {
      expect(res.validFile).toBe(true);
      expect(res.totalRowsDetected).toBe(1);
    });

    const req = httpMock.expectOne(`${baseUrl}/batches/validate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush(mockResponse);
  });

  it('uploadBatch should POST to /batches with multipart form data', () => {
    const mockFile = new File(['test'], 'feedback.csv', { type: 'text/csv' });
    const mockSummary: BatchSummary = {
      batchId: 'batch-123',
      fileName: 'feedback.csv',
      sourceType: 'CSV',
      status: 'COMPLETED',
      totalRows: 10,
      validRows: 10,
      successfulRows: 10,
      failedRows: 0,
      duplicateRows: 0,
      uploadedAt: '2026-11-02T10:00:00Z',
      fileHash: 'sha256-hash'
    };

    service.uploadBatch(mockFile, 'CSV', 'admin').subscribe(res => {
      expect(res.batchId).toBe('batch-123');
      expect(res.status).toBe('COMPLETED');
      expect(res.successfulRows).toBe(10);
    });

    const req = httpMock.expectOne(`${baseUrl}/batches`);
    expect(req.request.method).toBe('POST');
    req.flush(mockSummary);
  });

  it('listBatches should GET /batches with pagination and status filter', () => {
    const mockPaged: PagedResponse<BatchSummary> = {
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      last: true
    };

    service.listBatches(1, 10, 'COMPLETED').subscribe(res => {
      expect(res.page).toBe(0);
      expect(res.content).toEqual([]);
    });

    const req = httpMock.expectOne(`${baseUrl}/batches?page=1&size=10&status=COMPLETED`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPaged);
  });

  it('getBatchDetail should GET /batches/:batchId', () => {
    const mockDetail: BatchDetail = {
      batchId: 'b-999',
      fileName: 'sample.csv',
      fileHash: 'hash999',
      sourceType: 'CSV',
      uploadedBy: 'user1',
      status: 'COMPLETED',
      totalRows: 5,
      validRows: 5,
      successfulRows: 5,
      failedRows: 0,
      duplicateRows: 0,
      uploadedAt: '2026-11-02T10:00:00Z',
      createdAt: '2026-11-02T10:00:00Z',
      updatedAt: '2026-11-02T10:01:00Z'
    };

    service.getBatchDetail('b-999').subscribe(res => {
      expect(res.batchId).toBe('b-999');
      expect(res.fileName).toBe('sample.csv');
    });

    const req = httpMock.expectOne(`${baseUrl}/batches/b-999`);
    expect(req.request.method).toBe('GET');
    req.flush(mockDetail);
  });

  it('getBatchErrors should GET /batches/:batchId/errors with params', () => {
    const mockErrorsResponse: PagedResponse<ProcessingError> = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      last: true
    };

    service.getBatchErrors('b-999', 0, 20, 'ROW_VALIDATION', 'MISSING_FIELD').subscribe(res => {
      expect(res.totalElements).toBe(0);
    });

    const req = httpMock.expectOne(`${baseUrl}/batches/b-999/errors?page=0&size=20&stage=ROW_VALIDATION&errorCode=MISSING_FIELD`);
    expect(req.request.method).toBe('GET');
    req.flush(mockErrorsResponse);
  });

  it('listFeedback should GET /feedback with filters', () => {
    const mockFeedbackResponse: PagedResponse<Feedback> = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      last: true
    };

    service.listFeedback({ channel: 'web', rating: 5 }, 0, 20).subscribe(res => {
      expect(res.content.length).toBe(0);
    });

    const req = httpMock.expectOne(`${baseUrl}/feedback?page=0&size=20&channel=web&rating=5`);
    expect(req.request.method).toBe('GET');
    req.flush(mockFeedbackResponse);
  });

  it('getFeedbackDetail should GET /feedback/:feedbackId', () => {
    const mockFb: Feedback = {
      feedbackId: 'fb-123',
      batchId: 'b-999',
      externalReference: 'F0001',
      customerReference: 'C001',
      feedbackText: 'Sample text',
      feedbackDate: '2026-05-21',
      channel: 'web',
      rating: 4,
      product: 'Mobile Banking',
      region: 'North',
      customerSegment: 'Retail',
      language: 'en',
      processingStatus: 'STORED',
      receivedAt: '2026-11-02T10:00:00Z',
      deduplicationHash: 'hash999'
    };

    service.getFeedbackDetail('fb-123').subscribe(res => {
      expect(res.feedbackId).toBe('fb-123');
      expect(res.externalReference).toBe('F0001');
    });

    const req = httpMock.expectOne(`${baseUrl}/feedback/fb-123`);
    expect(req.request.method).toBe('GET');
    req.flush(mockFb);
  });
});
