import logging
from typing import Any, Dict, Optional

from app.agents.analytics_agent import AnalyticsAgent
from app.graph.graph_state import AgentGraphState

logger = logging.getLogger(__name__)


class AnalyticsNode:
    """
    LangGraph node orchestrating the Analytics Agent.
    Retrieves database aggregates, sentiment, category, and priority breakdowns.
    """

    def __init__(self, agent: Optional[AnalyticsAgent] = None):
        self.agent = agent or AnalyticsAgent()
        self.node_name = "analytics_node"

    def __call__(self, state: AgentGraphState) -> Dict[str, Any]:
        logger.info("Executing %s", self.node_name)
        evidence = self.agent.run()
        return {
            "analytics_results": evidence,
            "supporting_evidence": evidence,
            "execution_path": [self.node_name],
            "agents_used": [self.agent.name],
        }


def analytics_node(state: AgentGraphState) -> Dict[str, Any]:
    """Functional LangGraph node handler for analytics."""
    return AnalyticsNode()(state)
