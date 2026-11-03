export interface StoredEmbedding {
  embeddingId: string;
  feedbackId: string;
  embeddingModel: string;
  embeddingDimension: number;
  isCurrent: boolean;
  createdAt: string;
}

export interface BatchEmbeddingSummary {
  batchId: string;
  processed: number;
  successful: number;
  failed: number;
  durationSeconds: number;
}

export interface SemanticSearchResult {
  feedbackId: string;
  batchId: string;
  feedbackText: string;
  sentiment: string | null;
  category: string | null;
  theme: string | null;
  emotion: string | null;
  priority: string | null;
  summary: string | null;
  similarity: number;
}
