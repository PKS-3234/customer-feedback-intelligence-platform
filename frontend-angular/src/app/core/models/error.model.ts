export type ProcessingStage =
  | 'FILE_VALIDATION'
  | 'CSV_PARSING'
  | 'ROW_VALIDATION'
  | 'DUPLICATE_CHECK'
  | 'DATABASE_STORAGE'
  | 'AI_ANALYSIS'
  | 'EMBEDDING_GENERATION';

export interface ProcessingError {
  errorId: string;
  batchId: string;
  feedbackId?: string;
  csvRowNumber?: number;
  processingStage: ProcessingStage;
  errorCode: string;
  errorMessage: string;
  originalRow?: string;
  createdAt: string;
  resolved: boolean;
  resolvedAt?: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  errorCode: string;
  message: string;
  path: string;
  correlationId: string;
  fieldErrors?: Record<string, string>;
}
