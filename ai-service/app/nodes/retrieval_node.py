import logging
from typing import Any, Dict, Optional

from app.agents.retrieval_agent import RetrievalAgent
from app.graph.graph_state import AgentGraphState

logger = logging.getLogger(__name__)


class RetrievalNode:
    """
    LangGraph node orchestrating the Retrieval Agent.
    Executes semantic vector search over customer feedback.
    """

    def __init__(self, agent: Optional[RetrievalAgent] = None):
        self.agent = agent or RetrievalAgent()
        self.node_name = "retrieval_node"

    def __call__(self, state: AgentGraphState) -> Dict[str, Any]:
        query = state.get("query") or state.get("question", "")
        limit = state.get("limit", 5)
        logger.info("Executing %s with limit=%d", self.node_name, limit)
        evidence = self.agent.run(query=query, limit=limit)
        return {
            "retrieval_results": evidence,
            "supporting_evidence": evidence,
            "execution_path": [self.node_name],
            "agents_used": [self.agent.name],
        }


def retrieval_node(state: AgentGraphState) -> Dict[str, Any]:
    """Functional LangGraph node handler for retrieval."""
    return RetrievalNode()(state)
