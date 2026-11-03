import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AnalysisService } from '../../core/services/analysis.service';
import {
  BatchEmbeddingSummary,
  SemanticSearchResult
} from '../../core/models/embedding.model';

@Component({
  selector: 'app-semantic-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './semantic-search.component.html',
  styleUrl: './semantic-search.component.scss'
})
export class SemanticSearchComponent {
  query = '';
  resultLimit = 10;
  batchId = '';
  embeddingLimit: number | null = null;
  results: SemanticSearchResult[] = [];
  embeddingSummary: BatchEmbeddingSummary | null = null;
  isSearching = false;
  isEmbedding = false;
  errorMessage = '';

  constructor(
    private analysisService: AnalysisService,
    private changeDetector: ChangeDetectorRef
  ) {}

  search(): void {
    const normalizedQuery = this.query.trim();
    if (normalizedQuery.length < 2) {
      this.errorMessage = 'Enter at least two characters to search.';
      return;
    }
    if (!Number.isInteger(this.resultLimit) || this.resultLimit < 1 || this.resultLimit > 50) {
      this.errorMessage = 'Results limit must be a whole number from 1 to 50.';
      return;
    }

    this.errorMessage = '';
    this.isSearching = true;
    this.analysisService.semanticSearch(normalizedQuery, this.resultLimit).subscribe({
      next: (results) => {
        this.results = results;
        this.isSearching = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Semantic search failed.';
        this.isSearching = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  generateBatchEmbeddings(): void {
    const id = this.batchId.trim();
    if (!id) {
      this.errorMessage = 'Enter a batch ID to generate embeddings.';
      return;
    }
    if (this.embeddingLimit !== null &&
        (!Number.isInteger(this.embeddingLimit) || this.embeddingLimit < 1)) {
      this.errorMessage = 'Embedding limit must be a positive whole number.';
      return;
    }

    this.errorMessage = '';
    this.embeddingSummary = null;
    this.isEmbedding = true;
    this.analysisService.embedBatch(id, this.embeddingLimit ?? undefined).subscribe({
      next: (summary) => {
        this.embeddingSummary = summary;
        this.isEmbedding = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Batch embedding generation failed.';
        this.isEmbedding = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  searchFromFeedback(feedbackText: string): void {
    this.query = feedbackText;
    this.search();
  }
}
