export type AnalysisSentiment = 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';
export type AnalysisPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface FeedbackAnalysis {
  analysisId: string;
  feedbackId: string;
  sentiment: AnalysisSentiment;
  category: string;
  theme: string;
  emotion: string;
  priority: AnalysisPriority;
  assignedDepartment: string;
  summary: string;
  confidence: number;
  analyzedAt: string;
  modelName: string;
  promptVersion: string;
  isCurrent: boolean;
  createdAt: string;
}

export interface BatchAnalysisSummary {
  batchId: string;
  processed: number;
  successful: number;
  failed: number;
  durationSeconds: number;
}
