export interface AgentQueryRequest {
  query: string;
  limit?: number;
}

export interface SupportingEvidence {
  evidenceId: string;
  source: string;
  title: string;
  content: string;
  metadata: Record<string, unknown>;
}

export interface AgentQueryResponse {
  answer: string;
  agentsUsed: string[];
  supportingEvidence: SupportingEvidence[];
}
