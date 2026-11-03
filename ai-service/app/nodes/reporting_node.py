import logging
from typing import Any, Dict, Optional

from app.agents.reporting_agent import ReportingAgent
from app.graph.graph_state import AgentGraphState

logger = logging.getLogger(__name__)


class ReportingNode:
    """
    LangGraph node orchestrating the Reporting Agent.
    Synthesizes evidence from upstream specialist nodes into a grounded, cited executive response.
    """

    def __init__(self, agent: Optional[ReportingAgent] = None):
        self.agent = agent or ReportingAgent()
        self.node_name = "reporting_node"

    def __call__(self, state: AgentGraphState) -> Dict[str, Any]:
        query = state.get("query") or state.get("question", "")
        evidence = state.get("supporting_evidence", [])
        logger.info(
            "Executing %s with %d supporting evidence items",
            self.node_name,
            len(evidence),
        )
        answer = self.agent.run(query=query, evidence=evidence)
        return {
            "final_response": answer,
            "answer": answer,
            "execution_path": [self.node_name],
            "agents_used": [self.agent.name],
        }


def reporting_node(state: AgentGraphState) -> Dict[str, Any]:
    """Functional LangGraph node handler for reporting."""
    return ReportingNode()(state)
