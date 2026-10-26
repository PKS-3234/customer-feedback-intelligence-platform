export type ProcessingStatus = 'VALIDATED' | 'STORED' | 'AI_PENDING' | 'FAILED';

export interface Feedback {
  feedbackId: string;
  batchId: string;
  externalReference: string;
  customerReference?: string;
  feedbackText: string;
  feedbackDate: string;
  channel: string;
  rating?: number;
  product?: string;
  region?: string;
  customerSegment?: string;
  language?: string;
  receivedAt: string;
  processingStatus: ProcessingStatus;
  deduplicationHash: string;
}
