export interface DashboardChartDatum {
  label: string;
  count: number;
}

export interface DashboardEmbeddingModel {
  model: string;
  dimension: number | null;
  count: number;
}

export interface DashboardSemanticSummary {
  totalEmbeddings: number;
  models: DashboardEmbeddingModel[];
  vectorDimension: number | null;
  status: string;
  statusMessage: string;
}

export interface DashboardMetrics {
  totalFeedbackRecords: number;
  totalAnalyzedRecords: number;
  totalEmbeddedRecords: number;
  positiveFeedbackPercent: number;
  negativeFeedbackPercent: number;
  highPriorityFeedbackCount: number;
  currentBatches: number;
  processingErrors: number;
  unresolvedProcessingErrors: number;
  sentimentDistribution: DashboardChartDatum[];
  categoryDistribution: DashboardChartDatum[];
  themeDistribution: DashboardChartDatum[];
  priorityDistribution: DashboardChartDatum[];
  feedbackByChannel: DashboardChartDatum[];
  feedbackByProduct: DashboardChartDatum[];
  semanticSearch: DashboardSemanticSummary;
}

export interface DashboardAgentStatus {
  agent: string;
  purpose: string;
  availability: string;
  lastExecution: string | null;
}
