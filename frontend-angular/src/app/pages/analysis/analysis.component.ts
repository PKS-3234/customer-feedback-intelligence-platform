import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AnalysisService } from '../../core/services/analysis.service';
import { BatchAnalysisSummary, FeedbackAnalysis } from '../../core/models/analysis.model';

@Component({
  selector: 'app-analysis',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './analysis.component.html',
  styleUrl: './analysis.component.scss'
})
export class AnalysisComponent {
  batchId = '';
  limit: number | null = null;
  results: FeedbackAnalysis[] = [];
  summary: BatchAnalysisSummary | null = null;
  isLoading = false;
  isAnalyzing = false;
  errorMessage = '';

  constructor(
    private analysisService: AnalysisService,
    private changeDetector: ChangeDetectorRef
  ) {}

  loadResults(): void {
    const id = this.batchId.trim();
    if (!id) {
      this.errorMessage = 'Enter a batch ID to load its analysis results.';
      return;
    }

    this.errorMessage = '';
    this.summary = null;
    this.isLoading = true;
    this.analysisService.getBatchAnalysis(id).subscribe({
      next: (results) => {
        this.results = results;
        this.isLoading = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.results = [];
        this.errorMessage = error.error?.message || 'Failed to load analysis results.';
        this.isLoading = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  runBatchAnalysis(): void {
    const id = this.batchId.trim();
    if (!id) {
      this.errorMessage = 'Enter a batch ID before starting analysis.';
      return;
    }
    if (this.limit !== null && (!Number.isInteger(this.limit) || this.limit < 1)) {
      this.errorMessage = 'The analysis limit must be a positive whole number.';
      return;
    }

    this.errorMessage = '';
    this.summary = null;
    this.isAnalyzing = true;
    this.analysisService.analyzeBatch(id, this.limit ?? undefined).subscribe({
      next: (summary) => {
        this.summary = summary;
        this.changeDetector.markForCheck();
        this.analysisService.getBatchAnalysis(id).subscribe({
          next: (results) => {
            this.results = results;
            this.isAnalyzing = false;
            this.changeDetector.markForCheck();
          },
          error: (error) => {
            this.results = [];
            this.errorMessage = error.error?.message || 'Analysis finished, but results could not be loaded.';
            this.isAnalyzing = false;
            this.changeDetector.markForCheck();
          }
        });
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to start batch analysis.';
        this.isAnalyzing = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  reanalyze(feedbackId: string): void {
    this.errorMessage = '';
    this.analysisService.analyzeFeedback(feedbackId).subscribe({
      next: () => this.loadResults(),
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to re-analyze feedback.';
        this.changeDetector.markForCheck();
      }
    });
  }
}
