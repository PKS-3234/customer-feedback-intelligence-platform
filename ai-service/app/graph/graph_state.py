import operator
from typing import Annotated, Any, Dict, List, Optional, TypedDict

from app.models.agents import SupportingEvidence


class AgentGraphState(TypedDict, total=False):
    """
    LangGraph state schema for multi-agent feedback intelligence orchestration.

    State fields:
    - user question: query, question, limit
    - selected agents: selected_agents
    - analytics results: analytics_results
    - retrieval results: retrieval_results
    - quality results: quality_results
    - supporting evidence: supporting_evidence (accumulated across nodes)
    - final response: final_response, answer
    - execution path & agents used: execution_path, agents_used
    """

    # User question
    query: str
    question: str
    limit: int

    # Selected agents
    selected_agents: List[str]

    # Node-specific results
    analytics_results: List[SupportingEvidence]
    retrieval_results: List[SupportingEvidence]
    quality_results: List[SupportingEvidence]

    # Supporting evidence accumulated across all executed nodes
    supporting_evidence: Annotated[List[SupportingEvidence], operator.add]

    # Final response / answer
    final_response: Optional[str]
    answer: Optional[str]

    # Execution path and agent identifiers
    execution_path: Annotated[List[str], operator.add]
    agents_used: Annotated[List[str], operator.add]


def create_initial_state(
    query: str,
    limit: int = 5,
    selected_agents: Optional[List[str]] = None,
) -> AgentGraphState:
    """Creates a cleanly initialized AgentGraphState."""
    normalized_query = query.strip()
    return {
        "query": normalized_query,
        "question": normalized_query,
        "limit": limit,
        "selected_agents": list(selected_agents) if selected_agents else [],
        "analytics_results": [],
        "retrieval_results": [],
        "quality_results": [],
        "supporting_evidence": [],
        "final_response": None,
        "answer": None,
        "execution_path": [],
        "agents_used": [],
    }
