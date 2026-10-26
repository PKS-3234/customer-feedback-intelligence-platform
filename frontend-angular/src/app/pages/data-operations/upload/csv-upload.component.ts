import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { BatchService } from '../../../core/services/batch.service';
import { ValidateCsvResponse, RowValidationError } from '../../../core/models/validation.model';
import { BatchSummary } from '../../../core/models/batch.model';

@Component({
  selector: 'app-csv-upload',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './csv-upload.component.html',
  styleUrl: './csv-upload.component.scss'
})
export class CsvUploadComponent {

  selectedFile: File | null = null;
  fileSizeFormatted = '';
  isDragging = false;

  isValidating = false;
  isUploading = false;

  validationResult: ValidateCsvResponse | null = null;
  uploadResult: BatchSummary | null = null;
  errorMessage = '';

  constructor(private batchService: BatchService) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.handleFile(event.dataTransfer.files[0]);
    }
  }

  private handleFile(file: File): void {
    this.errorMessage = '';
    this.validationResult = null;
    this.uploadResult = null;

    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.errorMessage = 'Please select a valid CSV file (.csv).';
      this.selectedFile = null;
      return;
    }

    if (file.size === 0) {
      this.errorMessage = 'Selected file is empty.';
      this.selectedFile = null;
      return;
    }

    this.selectedFile = file;
    this.fileSizeFormatted = this.formatBytes(file.size);
  }

  validateFile(): void {
    if (!this.selectedFile) return;

    this.isValidating = true;
    this.errorMessage = '';
    this.validationResult = null;

    this.batchService.validateCsv(this.selectedFile).subscribe({
      next: (res) => {
        this.validationResult = res;
        this.isValidating = false;
      },
      error: (err) => {
        this.isValidating = false;
        this.errorMessage = err.error?.message || 'CSV validation failed. Please check file formatting.';
      }
    });
  }

  uploadFile(): void {
    if (!this.selectedFile || !this.canUpload) return;

    this.isUploading = true;
    this.errorMessage = '';

    this.batchService.uploadBatch(this.selectedFile).subscribe({
      next: (summary) => {
        this.uploadResult = summary;
        this.isUploading = false;
      },
      error: (err) => {
        this.isUploading = false;
        this.errorMessage = err.error?.message || 'Ingestion failed. Please check server logs.';
      }
    });
  }

  reset(): void {
    this.selectedFile = null;
    this.fileSizeFormatted = '';
    this.validationResult = null;
    this.uploadResult = null;
    this.errorMessage = '';
  }

  get canUpload(): boolean {
    return !!this.selectedFile && !!this.validationResult && !this.isValidating && !this.isUploading;
  }

  private formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}
