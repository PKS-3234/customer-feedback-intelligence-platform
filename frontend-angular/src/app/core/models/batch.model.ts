export type BatchStatus =
  | 'RECEIVED'
  | 'VALIDATING'
  | 'VALIDATED'
  | 'IMPORTING'
  | 'COMPLETED'
  | 'COMPLETED_WITH_ERRORS'
  | 'FAILED';

export type SourceType = 'CSV';

export interface BatchSummary {
  batchId: string;
  fileName: string;
  sourceType: SourceType;
  uploadedAt: string;
  totalRows: number;
  validRows: number;
  successfulRows: number;
  failedRows: number;
  duplicateRows: number;
  status: BatchStatus;
  fileHash: string;
  uploadedBy?: string;
}

export interface BatchDetail extends BatchSummary {
  createdAt: string;
  updatedAt: string;
}
