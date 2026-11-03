import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BatchAnalysisSummary, FeedbackAnalysis } from '../models/analysis.model';
import {
  BatchEmbeddingSummary,
  SemanticSearchResult,
  StoredEmbedding
} from '../models/embedding.model';

@Injectable({
  providedIn: 'root'
})
export class AnalysisService {
  private readonly baseUrl = `${environment.apiBaseUrl}/analysis`;

  constructor(private http: HttpClient) {}

  analyzeFeedback(feedbackId: string): Observable<FeedbackAnalysis> {
    return this.http.post<FeedbackAnalysis>(
      `${this.baseUrl}/feedback/${encodeURIComponent(feedbackId)}`,
      null
    );
  }

  analyzeBatch(batchId: string, limit?: number): Observable<BatchAnalysisSummary> {
    let params = new HttpParams();
    if (limit !== undefined) {
      params = params.set('limit', limit);
    }

    return this.http.post<BatchAnalysisSummary>(
      `${this.baseUrl}/batches/${encodeURIComponent(batchId)}`,
      null,
      { params }
    );
  }

  getFeedbackAnalysis(feedbackId: string): Observable<FeedbackAnalysis> {
    return this.http.get<FeedbackAnalysis>(
      `${this.baseUrl}/feedback/${encodeURIComponent(feedbackId)}`
    );
  }

  getBatchAnalysis(batchId: string): Observable<FeedbackAnalysis[]> {
    return this.http.get<FeedbackAnalysis[]>(
      `${this.baseUrl}/batches/${encodeURIComponent(batchId)}`
    );
  }

  embedFeedback(feedbackId: string): Observable<StoredEmbedding> {
    return this.http.post<StoredEmbedding>(
      `${environment.apiBaseUrl}/embeddings/feedback/${encodeURIComponent(feedbackId)}`,
      null
    );
  }

  getFeedbackEmbedding(feedbackId: string): Observable<StoredEmbedding> {
    return this.http.get<StoredEmbedding>(
      `${environment.apiBaseUrl}/embeddings/feedback/${encodeURIComponent(feedbackId)}`
    );
  }

  embedBatch(batchId: string, limit?: number): Observable<BatchEmbeddingSummary> {
    let params = new HttpParams();
    if (limit !== undefined) {
      params = params.set('limit', limit);
    }

    return this.http.post<BatchEmbeddingSummary>(
      `${environment.apiBaseUrl}/embeddings/batches/${encodeURIComponent(batchId)}`,
      null,
      { params }
    );
  }

  semanticSearch(query: string, limit = 10): Observable<SemanticSearchResult[]> {
    return this.http.post<SemanticSearchResult[]>(
      `${environment.apiBaseUrl}/semantic-search`,
      { query, limit }
    );
  }
}
