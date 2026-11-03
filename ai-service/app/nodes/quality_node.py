import logging
from typing import Any, Dict, Optional

from app.agents.data_quality_agent import DataQualityAgent
from app.graph.graph_state import AgentGraphState

logger = logging.getLogger(__name__)


class QualityNode:
    """
    LangGraph node orchestrating the Data Quality Agent.
    Inspects coverage metrics, unanalyzed/unembedded records, and errors.
    """

    def __init__(self, agent: Optional[DataQualityAgent] = None):
        self.agent = agent or DataQualityAgent()
        self.node_name = "quality_node"

    def __call__(self, state: AgentGraphState) -> Dict[str, Any]:
        logger.info("Executing %s", self.node_name)
        evidence = self.agent.run()
        return {
            "quality_results": evidence,
            "supporting_evidence": evidence,
            "execution_path": [self.node_name],
            "agents_used": [self.agent.name],
        }


def quality_node(state: AgentGraphState) -> Dict[str, Any]:
    """Functional LangGraph node handler for data quality."""
    return QualityNode()(state)
