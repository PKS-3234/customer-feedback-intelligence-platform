import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BatchDetail, BatchStatus, BatchSummary } from '../models/batch.model';
import { ValidateCsvResponse } from '../models/validation.model';
import { ProcessingError } from '../models/error.model';
import { Feedback } from '../models/feedback.model';
import { PagedResponse } from '../models/pagination.model';

@Injectable({
  providedIn: 'root'
})
export class BatchService {

  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  /**
   * Previews and validates a CSV file without persisting to database.
   */
  validateCsv(file: File): Observable<ValidateCsvResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ValidateCsvResponse>(`${this.baseUrl}/batches/validate`, formData);
  }

  /**
   * Uploads and ingests a CSV batch.
   */
  uploadBatch(file: File, sourceType = 'CSV', uploadedBy = 'web_user'): Observable<BatchSummary> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('sourceType', sourceType);
    formData.append('uploadedBy', uploadedBy);
    return this.http.post<BatchSummary>(`${this.baseUrl}/batches`, formData);
  }

  /**
   * Lists batches with optional status filter and pagination.
   */
  listBatches(page = 0, size = 10, status?: BatchStatus): Observable<PagedResponse<BatchSummary>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PagedResponse<BatchSummary>>(`${this.baseUrl}/batches`, { params });
  }

  /**
   * Gets single batch metadata and summary counts.
   */
  getBatchDetail(batchId: string): Observable<BatchDetail> {
    return this.http.get<BatchDetail>(`${this.baseUrl}/batches/${batchId}`);
  }

  /**
   * Gets paginated errors for a batch.
   */
  getBatchErrors(
    batchId: string,
    page = 0,
    size = 20,
    stage?: string,
    errorCode?: string
  ): Observable<PagedResponse<ProcessingError>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (stage) {
      params = params.set('stage', stage);
    }
    if (errorCode) {
      params = params.set('errorCode', errorCode);
    }

    return this.http.get<PagedResponse<ProcessingError>>(`${this.baseUrl}/batches/${batchId}/errors`, { params });
  }

  /**
   * Lists ingested feedback with optional filters.
   */
  listFeedback(filters: Record<string, any> = {}, page = 0, size = 20): Observable<PagedResponse<Feedback>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    Object.keys(filters).forEach(key => {
      const val = filters[key];
      if (val !== undefined && val !== null && val !== '') {
        params = params.set(key, val.toString());
      }
    });

    return this.http.get<PagedResponse<Feedback>>(`${this.baseUrl}/feedback`, { params });
  }

  /**
   * Gets single raw feedback record by ID.
   */
  getFeedbackDetail(feedbackId: string): Observable<Feedback> {
    return this.http.get<Feedback>(`${this.baseUrl}/feedback/${feedbackId}`);
  }
}
