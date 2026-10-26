import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { BatchService } from '../../../core/services/batch.service';
import { BatchStatus, BatchSummary } from '../../../core/models/batch.model';
import { PagedResponse } from '../../../core/models/pagination.model';

@Component({
  selector: 'app-batch-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './batch-list.component.html',
  styleUrl: './batch-list.component.scss'
})
export class BatchListComponent implements OnInit {

  batches: BatchSummary[] = [];
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;

  selectedStatus: BatchStatus | undefined = undefined;
  isLoading = false;
  errorMessage = '';

  readonly statusOptions: Array<{ label: string; value: BatchStatus | undefined }> = [
    { label: 'All Statuses', value: undefined },
    { label: 'Completed', value: 'COMPLETED' },
    { label: 'Completed With Errors', value: 'COMPLETED_WITH_ERRORS' },
    { label: 'Failed', value: 'FAILED' },
    { label: 'Importing', value: 'IMPORTING' }
  ];

  constructor(private batchService: BatchService) {}

  ngOnInit(): void {
    this.loadBatches();
  }

  loadBatches(pageNumber = 0): void {
    this.page = pageNumber;
    this.isLoading = true;
    this.errorMessage = '';

    this.batchService.listBatches(this.page, this.size, this.selectedStatus).subscribe({
      next: (res: PagedResponse<BatchSummary>) => {
        this.batches = res.content;
        this.totalElements = res.totalElements;
        this.totalPages = res.totalPages;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to load batches list.';
      }
    });
  }

  onStatusFilterChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const val = select.value;
    this.selectedStatus = val ? (val as BatchStatus) : undefined;
    this.loadBatches(0);
  }

  previousPage(): void {
    if (this.page > 0) {
      this.loadBatches(this.page - 1);
    }
  }

  nextPage(): void {
    if (this.page + 1 < this.totalPages) {
      this.loadBatches(this.page + 1);
    }
  }
}
