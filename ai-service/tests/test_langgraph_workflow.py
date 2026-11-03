from unittest.mock import MagicMock
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

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
from app.graph.graph_state import AgentGraphState, create_initial_state
from app.main import app
from app.models.agents import AgentQueryRequest, SupportingEvidence
from app.nodes.analytics_node import AnalyticsNode
from app.nodes.quality_node import QualityNode
from app.nodes.reporting_node import ReportingNode
from app.nodes.retrieval_node import RetrievalNode
from app.nodes.routing_node import RoutingNode, route_next_nodes
from app.workflow.langgraph_workflow import LangGraphWorkflow

client = TestClient(app)


# ---------------------------------------------------------------------------
# State and Graph Builder Unit Tests
# ---------------------------------------------------------------------------

def test_graph_state_initialization_supports_all_required_fields():
    state = create_initial_state(query="Explain duplicate charge trends", limit=7)

    assert state["query"] == "Explain duplicate charge trends"
    assert state["question"] == "Explain duplicate charge trends"
    assert state["limit"] == 7
    assert state["selected_agents"] == []
    assert state["analytics_results"] == []
    assert state["retrieval_results"] == []
    assert state["quality_results"] == []
    assert state["supporting_evidence"] == []
    assert state["final_response"] is None
    assert state["answer"] is None
    assert state["execution_path"] == []
    assert state["agents_used"] == []


def test_graph_builder_compiles_with_all_registered_nodes():
    graph = build_feedback_graph()
    assert graph is not None

    meta = get_graph_metadata(graph)
    assert meta["node_names"] == REGISTERED_NODE_NAMES
    assert len(meta["nodes"]) == 5
    assert len(meta["edges"]) >= 8
    assert "routing_node" in meta["mermaid"]
    assert "reporting_node" in meta["mermaid"]


# ---------------------------------------------------------------------------
# Routing and Conditional Edge Tests
# ---------------------------------------------------------------------------

@pytest.mark.parametrize(
    ("selected", "expected_targets"),
    [
        (["analytics"], [NODE_ANALYTICS]),
        (["retrieval"], [NODE_RETRIEVAL]),
        (["quality"], [NODE_QUALITY]),
        (["analytics", "retrieval"], [NODE_ANALYTICS, NODE_RETRIEVAL]),
        (["analytics", "quality"], [NODE_ANALYTICS, NODE_QUALITY]),
        (["retrieval", "quality"], [NODE_RETRIEVAL, NODE_QUALITY]),
        (["analytics", "retrieval", "quality"], [NODE_ANALYTICS, NODE_RETRIEVAL, NODE_QUALITY]),
        ([], [NODE_REPORTING]),
    ],
)
def test_conditional_edge_routing_logic(selected, expected_targets):
    state: AgentGraphState = {"selected_agents": selected}
    targets = route_next_nodes(state)
    assert targets == expected_targets


def test_routing_node_populates_selected_agents_and_execution_path():
    state = create_initial_state("How many negative reviews are there?")
    result = RoutingNode()(state)

    assert result["selected_agents"] == ["analytics"]
    assert result["execution_path"] == [NODE_ROUTING]
    assert result["agents_used"] == ["Supervisor Agent"]


# ---------------------------------------------------------------------------
# Individual Node Execution Tests
# ---------------------------------------------------------------------------

def test_analytics_node_invokes_analytics_agent():
    mock_agent = MagicMock()
    mock_agent.name = "Analytics Agent"
    mock_evidence = [
        SupportingEvidence(
            evidenceId="analytics-overview",
            source="feedback",
            title="Feedback totals",
            content="10 records.",
        )
    ]
    mock_agent.run.return_value = mock_evidence

    node = AnalyticsNode(mock_agent)
    res = node(create_initial_state("Show sentiment breakdown"))

    assert res["analytics_results"] == mock_evidence
    assert res["supporting_evidence"] == mock_evidence
    assert res["execution_path"] == [NODE_ANALYTICS]
    assert res["agents_used"] == ["Analytics Agent"]
    mock_agent.run.assert_called_once_with()


def test_retrieval_node_invokes_retrieval_agent_with_query_and_limit():
    mock_agent = MagicMock()
    mock_agent.name = "Retrieval Agent"
    feedback_id = uuid4()
    mock_evidence = [
        SupportingEvidence(
            evidenceId=f"feedback-{feedback_id}",
            source="feedback",
            title="Similar feedback",
            content="Account logged out unexpectedly.",
        )
    ]
    mock_agent.run.return_value = mock_evidence

    node = RetrievalNode(mock_agent)
    res = node(create_initial_state("Find login issues", limit=3))

    assert res["retrieval_results"] == mock_evidence
    assert res["supporting_evidence"] == mock_evidence
    assert res["execution_path"] == [NODE_RETRIEVAL]
    assert res["agents_used"] == ["Retrieval Agent"]
    mock_agent.run.assert_called_once_with(query="Find login issues", limit=3)


def test_quality_node_invokes_data_quality_agent():
    mock_agent = MagicMock()
    mock_agent.name = "Data Quality Agent"
    mock_evidence = [
        SupportingEvidence(
            evidenceId="quality-coverage",
            source="feedback",
            title="Coverage",
            content="100% analyzed.",
        )
    ]
    mock_agent.run.return_value = mock_evidence

    node = QualityNode(mock_agent)
    res = node(create_initial_state("Check data quality"))

    assert res["quality_results"] == mock_evidence
    assert res["supporting_evidence"] == mock_evidence
    assert res["execution_path"] == [NODE_QUALITY]
    assert res["agents_used"] == ["Data Quality Agent"]
    mock_agent.run.assert_called_once_with()


def test_reporting_node_invokes_reporting_agent_and_synthesizes_response():
    mock_agent = MagicMock()
    mock_agent.name = "Reporting Agent"
    mock_agent.run.return_value = "Total 10 records analyzed [analytics-overview]."

    evidence = [
        SupportingEvidence(
            evidenceId="analytics-overview",
            source="feedback",
            title="Feedback totals",
            content="10 records.",
        )
    ]
    state = create_initial_state("How many records?")
    state["supporting_evidence"] = evidence

    node = ReportingNode(mock_agent)
    res = node(state)

    assert res["final_response"] == "Total 10 records analyzed [analytics-overview]."
    assert res["answer"] == "Total 10 records analyzed [analytics-overview]."
    assert res["execution_path"] == [NODE_REPORTING]
    assert res["agents_used"] == ["Reporting Agent"]
    mock_agent.run.assert_called_once_with(
        query="How many records?",
        evidence=evidence,
    )


# ---------------------------------------------------------------------------
# End-to-End Workflow Execution Tests (Question -> StateGraph -> Agent -> Response)
# ---------------------------------------------------------------------------

def test_workflow_executes_analytics_branch():
    analytics = MagicMock()
    analytics.name = "Analytics Agent"
    analytics.run.return_value = [
        SupportingEvidence(
            evidenceId="analytics-overview",
            source="feedback",
            title="Overview",
            content="5 records total.",
        )
    ]
    retrieval = MagicMock()
    retrieval.name = "Retrieval Agent"
    quality = MagicMock()
    quality.name = "Data Quality Agent"
    reporting = MagicMock()
    reporting.name = "Reporting Agent"
    reporting.run.return_value = "There are 5 feedback records [analytics-overview]."

    workflow = LangGraphWorkflow(
        analytics_agent=analytics,
        retrieval_agent=retrieval,
        data_quality_agent=quality,
        reporting_agent=reporting,
    )

    request = AgentQueryRequest(query="How many feedback records are there?")
    response = workflow.run(request)

    assert response.answer == "There are 5 feedback records [analytics-overview]."
    assert response.agentsUsed == [
        "Supervisor Agent",
        "Analytics Agent",
        "Reporting Agent",
    ]
    assert len(response.supportingEvidence) == 1
    assert response.supportingEvidence[0].evidenceId == "analytics-overview"

    analytics.run.assert_called_once()
    retrieval.run.assert_not_called()
    quality.run.assert_not_called()
    reporting.run.assert_called_once()


def test_workflow_executes_retrieval_branch():
    analytics = MagicMock()
    retrieval = MagicMock()
    retrieval.name = "Retrieval Agent"
    feedback_id = uuid4()
    retrieval.run.return_value = [
        SupportingEvidence(
            evidenceId=f"feedback-{feedback_id}",
            source="feedback",
            title="Similar feedback",
            content="Card charged twice.",
            metadata={"similarity": 0.95},
        )
    ]
    quality = MagicMock()
    reporting = MagicMock()
    reporting.name = "Reporting Agent"
    reporting.run.return_value = f"Found duplicate charge issue [feedback-{feedback_id}]."

    workflow = LangGraphWorkflow(
        analytics_agent=analytics,
        retrieval_agent=retrieval,
        data_quality_agent=quality,
        reporting_agent=reporting,
    )

    request = AgentQueryRequest(query="Find similar comments about duplicate card charge", limit=4)
    response = workflow.run(request)

    assert f"feedback-{feedback_id}" in response.answer
    assert response.agentsUsed == [
        "Supervisor Agent",
        "Retrieval Agent",
        "Reporting Agent",
    ]
    analytics.run.assert_not_called()
    quality.run.assert_not_called()
    retrieval.run.assert_called_once_with(
        query="Find similar comments about duplicate card charge",
        limit=4,
    )


def test_workflow_executes_quality_branch():
    analytics = MagicMock()
    retrieval = MagicMock()
    quality = MagicMock()
    quality.name = "Data Quality Agent"
    quality.run.return_value = [
        SupportingEvidence(
            evidenceId="quality-coverage",
            source="feedback",
            title="Coverage",
            content="All records embedded.",
        )
    ]
    reporting = MagicMock()
    reporting.name = "Reporting Agent"
    reporting.run.return_value = "Embedding coverage is complete [quality-coverage]."

    workflow = LangGraphWorkflow(
        analytics_agent=analytics,
        retrieval_agent=retrieval,
        data_quality_agent=quality,
        reporting_agent=reporting,
    )

    request = AgentQueryRequest(query="What is the embedding coverage status?")
    response = workflow.run(request)

    assert "quality-coverage" in response.answer
    assert response.agentsUsed == [
        "Supervisor Agent",
        "Data Quality Agent",
        "Reporting Agent",
    ]
    analytics.run.assert_not_called()
    retrieval.run.assert_not_called()
    quality.run.assert_called_once()


def test_workflow_multi_agent_fan_out_and_barrier_synchronization():
    analytics = MagicMock()
    analytics.name = "Analytics Agent"
    analytics.run.return_value = [
        SupportingEvidence(evidenceId="ev-analytics", source="feedback", title="Analytics", content="Count=20")
    ]
    retrieval = MagicMock()
    retrieval.name = "Retrieval Agent"
    retrieval.run.return_value = [
        SupportingEvidence(evidenceId="ev-retrieval", source="feedback", title="Retrieval", content="Example comment")
    ]
    quality = MagicMock()
    quality.name = "Data Quality Agent"
    quality.run.return_value = [
        SupportingEvidence(evidenceId="ev-quality", source="feedback", title="Quality", content="Quality 99%")
    ]
    reporting = MagicMock()
    reporting.name = "Reporting Agent"
    reporting.run.return_value = "Overall overview synthesized [ev-analytics] [ev-retrieval] [ev-quality]."

    workflow = LangGraphWorkflow(
        analytics_agent=analytics,
        retrieval_agent=retrieval,
        data_quality_agent=quality,
        reporting_agent=reporting,
    )

    # General query triggers all 3 specialist agents
    request = AgentQueryRequest(query="Give me an overview of customer feedback")
    response = workflow.run(request)

    assert "Overall overview synthesized" in response.answer
    assert "Supervisor Agent" in response.agentsUsed
    assert "Analytics Agent" in response.agentsUsed
    assert "Retrieval Agent" in response.agentsUsed
    assert "Data Quality Agent" in response.agentsUsed
    assert "Reporting Agent" in response.agentsUsed
    assert len(response.supportingEvidence) == 3

    analytics.run.assert_called_once()
    retrieval.run.assert_called_once()
    quality.run.assert_called_once()
    reporting.run.assert_called_once()


# ---------------------------------------------------------------------------
# Workflow Visibility API Endpoint Tests
# ---------------------------------------------------------------------------

def test_workflow_visibility_endpoint_returns_graph_structure():
    response = client.get("/api/v1/agents/workflow")
    assert response.status_code == 200

    data = response.json()
    assert "graph_structure" in data or "graphStructure" in data
    assert "node_names" in data or "nodeNames" in data
    assert "execution_path" in data or "executionPath" in data

    node_names = data.get("node_names") or data.get("nodeNames")
    assert set(node_names) == set(REGISTERED_NODE_NAMES)

    graph_structure = data.get("graph_structure") or data.get("graphStructure")
    assert "nodes" in graph_structure
    assert "edges" in graph_structure
    assert "mermaid" in graph_structure


def test_workflow_visibility_endpoint_with_query_parameter():
    response = client.get("/api/v1/agents/workflow", params={"query": "How many records?"})
    assert response.status_code == 200

    data = response.json()
    execution_path = data.get("execution_path") or data.get("executionPath")
    assert execution_path == [NODE_ROUTING, NODE_ANALYTICS, NODE_REPORTING]


def test_workflow_visibility_endpoint_internal_v1_prefix():
    response = client.get("/internal/v1/agents/workflow")
    assert response.status_code == 200
    data = response.json()
    assert data["node_names"] == REGISTERED_NODE_NAMES


# ---------------------------------------------------------------------------
# Module and Alias Compatibility Tests
# ---------------------------------------------------------------------------

def test_direct_and_aliased_imports():
    from graph.graph_builder import build_feedback_graph as bg1
    from app.graph.graph_builder import build_feedback_graph as bg2
    assert bg1 is bg2

    from graph.graph_state import AgentGraphState as gs1
    from app.graph.graph_state import AgentGraphState as gs2
    assert gs1 is gs2

    from nodes.routing_node import routing_node as rn1
    from app.nodes.routing_node import routing_node as rn2
    assert rn1 is rn2

    from workflow.langgraph_workflow import LangGraphWorkflow as lw1
    from app.workflow.langgraph_workflow import LangGraphWorkflow as lw2
    assert lw1 is lw2
