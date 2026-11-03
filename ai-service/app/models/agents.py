from typing import Any, Dict, List

from pydantic import BaseModel, Field


class AgentQueryRequest(BaseModel):
    query: str = Field(..., min_length=2, max_length=2000)
    limit: int = Field(default=5, ge=1, le=10)


class SupportingEvidence(BaseModel):
    evidenceId: str
    source: str
    title: str
    content: str
    metadata: Dict[str, Any] = Field(default_factory=dict)


class AgentQueryResponse(BaseModel):
    answer: str
    agentsUsed: List[str]
    supportingEvidence: List[SupportingEvidence]
