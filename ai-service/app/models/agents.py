from typing import Any, Dict, List, Optional

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


class WorkflowVisibilityResponse(BaseModel):
    graph_structure: Dict[str, Any] = Field(..., description="Graph nodes, edges, and mermaid diagram")
    node_names: List[str] = Field(..., description="Registered LangGraph node names")
    execution_path: List[str] = Field(..., description="Demonstration or evaluated execution path")
    graphStructure: Optional[Dict[str, Any]] = None
    nodeNames: Optional[List[str]] = None
    executionPath: Optional[List[str]] = None
