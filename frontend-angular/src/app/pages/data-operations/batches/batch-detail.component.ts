import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { BatchService } from '../../../core/services/batch.service';
import { BatchDetail } from '../../../core/models/batch.model';
import { ProcessingError, ProcessingStage } from '../../../core/models/error.model';
import { PagedResponse } from '../../../core/models/pagination.model';

@Component({
  selector: 'app-batch-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './batch-detail.component.html',
  styleUrl: './batch-detail.component.scss'
})
export class BatchDetailComponent implements OnInit {

  batchId = '';
  batch: BatchDetail | null = null;
  errors: ProcessingError[] = [];

  page = 0;
  size = 20;
  totalErrors = 0;
  totalPages = 0;

  selectedStage: string | undefined = undefined;
  isLoadingBatch = true;
  isLoadingErrors = false;
  errorMessage = '';

  expandedErrorRow: string | null = null;

  readonly stageOptions: Array<{ label: string; value: string | undefined }> = [
    { label: 'All Stages', value: undefined },
    { label: 'Row Validation', value: 'ROW_VALIDATION' },
    { label: 'Duplicate Check', value: 'DUPLICATE_CHECK' },
    { label: 'CSV Parsing', value: 'CSV_PARSING' },
    { label: 'File Validation', value: 'FILE_VALIDATION' }
  ];

  constructor(
    private route: ActivatedRoute,
    private batchService: BatchService
  ) {}

  ngOnInit(): void {
    this.batchId = this.route.snapshot.paramMap.get('batchId') || '';
    if (this.batchId) {
      this.loadBatch();
      this.loadErrors();
    } else {
      this.errorMessage = 'No valid batch ID supplied.';
      this.isLoadingBatch = false;
    }
  }

  loadBatch(): void {
    this.isLoadingBatch = true;
    this.batchService.getBatchDetail(this.batchId).subscribe({
      next: (b) => {
        this.batch = b;
        this.isLoadingBatch = false;
      },
      error: (err) => {
        this.isLoadingBatch = false;
        this.errorMessage = err.error?.message || 'Failed to load batch metadata.';
      }
    });
  }

  loadErrors(pageNumber = 0): void {
    this.page = pageNumber;
    this.isLoadingErrors = true;

    this.batchService.getBatchErrors(this.batchId, this.page, this.size, this.selectedStage).subscribe({
      next: (res: PagedResponse<ProcessingError>) => {
        this.errors = res.content;
        this.totalErrors = res.totalElements;
        this.totalPages = res.totalPages;
        this.isLoadingErrors = false;
      },
      error: (err) => {
        this.isLoadingErrors = false;
      }
    });
  }

  onStageFilterChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedStage = select.value || undefined;
    this.loadErrors(0);
  }

  toggleExpandRow(errorId: string): void {
    this.expandedErrorRow = this.expandedErrorRow === errorId ? null : errorId;
  }

  previousPage(): void {
    if (this.page > 0) {
      this.loadErrors(this.page - 1);
    }
  }

  nextPage(): void {
    if (this.page + 1 < this.totalPages) {
      this.loadErrors(this.page + 1);
    }
  }
}
