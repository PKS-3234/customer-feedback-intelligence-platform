import logging
from typing import Any, Dict, List, Optional

from app.agents.analytics_agent import AnalyticsAgent
from app.agents.data_quality_agent import DataQualityAgent
from app.agents.reporting_agent import ReportingAgent
from app.agents.retrieval_agent import RetrievalAgent
from app.agents.supervisor_agent import SupervisorAgent
from app.graph.graph_builder import (
    NODE_ANALYTICS,
    NODE_QUALITY,
    NODE_REPORTING,
    NODE_RETRIEVAL,
    NODE_ROUTING,
    REGISTERED_NODE_NAMES,
    build_feedback_graph,
    get_graph_metadata,
)
from app.graph.graph_state import create_initial_state
from app.models.agents import AgentQueryRequest, AgentQueryResponse

logger = logging.getLogger(__name__)


class LangGraphWorkflow:
    """
    LangGraph StateGraph workflow orchestrator for multi-agent feedback intelligence.
    Replaces manual supervisor-router procedural dispatch with a declarative StateGraph.
    """

    def __init__(
        self,
        analytics_agent: Optional[AnalyticsAgent] = None,
        retrieval_agent: Optional[RetrievalAgent] = None,
        data_quality_agent: Optional[DataQualityAgent] = None,
        reporting_agent: Optional[ReportingAgent] = None,
        graph=None,
    ):
        self.analytics_agent = analytics_agent
        self.retrieval_agent = retrieval_agent
        self.data_quality_agent = data_quality_agent
        self.reporting_agent = reporting_agent
        self.graph = graph or build_feedback_graph(
            analytics_agent=self.analytics_agent,
            retrieval_agent=self.retrieval_agent,
            data_quality_agent=self.data_quality_agent,
            reporting_agent=self.reporting_agent,
        )

    def run(self, request: AgentQueryRequest) -> AgentQueryResponse:
        """
        Executes user query through the LangGraph StateGraph workflow:
        Question -> StateGraph Execution -> Agent Invocation -> Response.
        """
        logger.info("Executing LangGraph workflow for query: '%s'", request.query)
        initial_state = create_initial_state(
            query=request.query,
            limit=request.limit,
        )

        final_state = self.graph.invoke(initial_state)

        answer = (
            final_state.get("final_response")
            or final_state.get("answer")
            or "I could not formulate an answer from the collected evidence."
        )
        agents_used = final_state.get("agents_used", [])
        evidence = final_state.get("supporting_evidence", [])

        logger.info(
            "LangGraph execution finished. Nodes traversed: %s; Agents: %s; Evidence items: %d",
            final_state.get("execution_path", []),
            agents_used,
            len(evidence),
        )

        return AgentQueryResponse(
            answer=answer,
            agentsUsed=agents_used,
            supportingEvidence=evidence,
        )

    def execute(self, request: AgentQueryRequest) -> AgentQueryResponse:
        """Alias for run() to maintain complete interface compatibility."""
        return self.run(request)

    def get_workflow_info(self, query: Optional[str] = None) -> Dict[str, Any]:
        """
        Returns graph structure, node names, and execution path for visibility and debugging.
        """
        meta = get_graph_metadata(self.graph)

        if query:
            # Determine execution path based on query routing
            routed = SupervisorAgent.route(query)
            path = [NODE_ROUTING]
            if "analytics" in routed:
                path.append(NODE_ANALYTICS)
            if "retrieval" in routed:
                path.append(NODE_RETRIEVAL)
            if "quality" in routed:
                path.append(NODE_QUALITY)
            if len(path) == 1:
                path.append(NODE_REPORTING)
            else:
                path.append(NODE_REPORTING)
        else:
            # Default demonstration execution path traversing all nodes
            path = [
                NODE_ROUTING,
                NODE_ANALYTICS,
                NODE_RETRIEVAL,
                NODE_QUALITY,
                NODE_REPORTING,
            ]

        graph_structure = {
            "nodes": meta["nodes"],
            "edges": meta["edges"],
            "mermaid": meta["mermaid"],
        }

        return {
            "graph_structure": graph_structure,
            "node_names": REGISTERED_NODE_NAMES,
            "execution_path": path,
            "graphStructure": graph_structure,
            "nodeNames": REGISTERED_NODE_NAMES,
            "executionPath": path,
        }
