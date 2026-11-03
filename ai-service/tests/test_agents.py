from unittest.mock import MagicMock, patch
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

from app.agents.reporting_agent import ReportingAgent
from app.agents.supervisor_agent import SupervisorAgent
from app.models.embeddings import SemanticSearchResult
from app.main import app
from app.models.agents import AgentQueryRequest, SupportingEvidence

client = TestClient(app)


@pytest.mark.parametrize(
    ("query", "expected"),
    [
        ("How many negative feedback records are there?", ["analytics"]),
        ("Find similar comments about duplicate charges", ["retrieval"]),
        ("What is the embedding coverage and data quality?", ["quality"]),
        ("Give me an overview of customer feedback", ["analytics", "retrieval", "quality"]),
    ],
)
def test_supervisor_routes_queries_deterministically(query, expected):
    assert SupervisorAgent.route(query) == expected


def test_supervisor_executes_selected_agents_then_reports():
    analytics = MagicMock()
    analytics.name = "Analytics Agent"
    analytics.run.return_value = [
        SupportingEvidence(
            evidenceId="analytics-overview",
            source="feedback",
            title="Feedback total",
            content="3 feedback records.",
        )
    ]
    retrieval = MagicMock()
    retrieval.name = "Retrieval Agent"
    quality = MagicMock()
    quality.name = "Data Quality Agent"
    reporting = MagicMock()
    reporting.name = "Reporting Agent"
    reporting.run.return_value = "There are 3 records [analytics-overview]."
    supervisor = SupervisorAgent(analytics, retrieval, quality, reporting)

    response = supervisor.execute(AgentQueryRequest(query="How many feedback records?"))

    assert response.answer == "There are 3 records [analytics-overview]."
    assert response.agentsUsed == ["Supervisor Agent", "Analytics Agent", "Reporting Agent"]
    assert response.supportingEvidence[0].evidenceId == "analytics-overview"
    analytics.run.assert_called_once_with()
    retrieval.run.assert_not_called()
    quality.run.assert_not_called()
    reporting.run.assert_called_once()


def test_retrieval_agent_returns_semantic_results_as_evidence():
    from app.agents.retrieval_agent import RetrievalAgent

    service = MagicMock()
    feedback_id = uuid4()
    batch_id = uuid4()
    service.search.return_value = [SemanticSearchResult(
        feedbackId=feedback_id,
        batchId=batch_id,
        feedbackText="Card was charged twice.",
        sentiment="NEGATIVE",
        category="Payments",
        theme="Duplicate Debit",
        emotion="Frustrated",
        priority="HIGH",
        summary="Duplicate charge.",
        similarity=0.94,
    )]
    evidence = RetrievalAgent(service).run("duplicate card charge", 3)

    assert evidence[0].evidenceId == f"feedback-{feedback_id}"
    assert evidence[0].content == "Card was charged twice."
    assert evidence[0].metadata["similarity"] == 0.94
    service.search.assert_called_once_with("duplicate card charge", 3)


def test_reporting_agent_uses_only_supplied_evidence_and_rejects_empty_response():
    provider = MagicMock()
    provider.generate.return_value = "Customers report duplicate charges [feedback-1]."
    reporter = ReportingAgent(provider)
    evidence = [
        SupportingEvidence(
            evidenceId="feedback-1",
            source="feedback",
            title="Similar feedback",
            content="Card charged twice.",
        )
    ]

    assert reporter.run("What issues?", evidence).startswith("Customers report")
    prompt = provider.generate.call_args.kwargs["prompt"]
    assert "Card charged twice." in prompt
    assert "Do not invent numbers" in prompt

    provider.generate.return_value = " "
    with pytest.raises(RuntimeError, match="empty answer"):
        reporter.run("What issues?", evidence)


@patch("app.api.v1.endpoints.agents.SupervisorAgent")
def test_agent_query_endpoint_returns_answer_agents_and_evidence(supervisor_class):
    supervisor = MagicMock()
    supervisor.execute.return_value = {
        "answer": "Customers report duplicate charges [feedback-1].",
        "agentsUsed": ["Supervisor Agent", "Retrieval Agent", "Reporting Agent"],
        "supportingEvidence": [{
            "evidenceId": "feedback-1",
            "source": "feedback",
            "title": "Similar feedback",
            "content": "Card charged twice.",
            "metadata": {"similarity": 0.94},
        }],
    }
    supervisor_class.return_value = supervisor

    response = client.post(
        "/api/v1/agents/query",
        json={"query": "Find duplicate charge feedback"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["answer"].startswith("Customers report")
    assert body["agentsUsed"][-1] == "Reporting Agent"
    assert body["supportingEvidence"][0]["content"] == "Card charged twice."
    assert supervisor.execute.call_args.args[0].limit == 5


@pytest.mark.parametrize(
    "body",
    [
        {"query": " "},
        {"query": "x"},
        {"query": "valid question", "limit": 0},
        {"query": "valid question", "limit": 11},
    ],
)
def test_agent_query_endpoint_validates_request(body):
    response = client.post("/api/v1/agents/query", json=body)
    assert response.status_code == 422
