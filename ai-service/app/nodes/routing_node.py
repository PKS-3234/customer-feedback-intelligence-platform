import logging
from typing import Any, Dict, List

from app.agents.supervisor_agent import SupervisorAgent
from app.graph.graph_state import AgentGraphState

logger = logging.getLogger(__name__)


class RoutingNode:
    """
    LangGraph node implementing supervisor routing.
    Evaluates user questions and determines which specialist agent nodes to invoke.
    """

    def __init__(self, name: str = "Supervisor Agent"):
        self.name = name

    def __call__(self, state: AgentGraphState) -> Dict[str, Any]:
        query = state.get("query") or state.get("question", "")
        # Use existing supervisor behavior to select agents
        selected = SupervisorAgent.route(query)
        logger.info("RoutingNode routed query '%s' to agents: %s", query, selected)
        return {
            "selected_agents": selected,
            "execution_path": ["routing_node"],
            "agents_used": [self.name],
        }


def routing_node(state: AgentGraphState) -> Dict[str, Any]:
    """Functional LangGraph node handler for routing."""
    return RoutingNode()(state)


def route_next_nodes(state: AgentGraphState) -> List[str]:
    """
    Conditional edge function routing from routing_node to specialist nodes or reporting.
    Returns the list of target node names to execute.
    """
    selected = state.get("selected_agents", [])
    targets: List[str] = []
    if "analytics" in selected:
        targets.append("analytics_node")
    if "retrieval" in selected:
        targets.append("retrieval_node")
    if "quality" in selected:
        targets.append("quality_node")

    # If no specialist nodes selected, route directly to reporting node
    if not targets:
        targets.append("reporting_node")

    logger.debug("Conditional edge resolved targets: %s", targets)
    return targets
