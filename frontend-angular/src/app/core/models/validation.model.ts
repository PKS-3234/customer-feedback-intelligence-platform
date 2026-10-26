export interface RowValidationError {
  rowNumber: number;
  field: string;
  errorCode: string;
  message: string;
}

export interface ValidateCsvResponse {
  validFile: boolean;
  fileName: string;
  detectedHeaders: string[];
  requiredHeadersFound: string[];
  missingHeaders: string[];
  totalRowsDetected: number;
  validRowCount: number;
  invalidRowCount: number;
  duplicateWithinFileCount: number;
  previewRows: Array<Record<string, string>>;
  validationErrors: RowValidationError[];
  proposedMapping: Record<string, string>;
}
