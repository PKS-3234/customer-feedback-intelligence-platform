from datetime import datetime, timezone
from unittest.mock import MagicMock, patch
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.models.embeddings import StoredEmbeddingResponse
from app.provider.gemini_embeddings import GeminiEmbeddingProvider
from app.service.embedding_service import FeedbackEmbeddingService
from app.workflow.embedding_workflow import EmbeddingWorkflow

client = TestClient(app)


def test_gemini_embedding_provider_returns_configured_vector():
    response = MagicMock()
    response.status_code = 200
    response.json.return_value = {"embedding": {"values": [0.1, -0.2, 0.3]}}
    with patch("httpx.Client.post", return_value=response) as post:
        provider = GeminiEmbeddingProvider(
            api_key="test-key",
            model_name="test-embedding-model",
            dimensions=3,
            retries=1,
        )

        assert provider.embed("feedback", task_type="RETRIEVAL_QUERY") == [0.1, -0.2, 0.3]
        kwargs = post.call_args.kwargs
        assert kwargs["json"]["taskType"] == "RETRIEVAL_QUERY"
        assert kwargs["json"]["outputDimensionality"] == 3
        assert kwargs["headers"]["x-goog-api-key"] == "test-key"


def test_gemini_embedding_provider_rejects_wrong_vector_dimension():
    response = MagicMock()
    response.status_code = 200
    response.json.return_value = {"embedding": {"values": [0.1]}}
    with patch("httpx.Client.post", return_value=response):
        provider = GeminiEmbeddingProvider(
            api_key="test-key",
            dimensions=3,
            retries=1,
        )
        with pytest.raises(RuntimeError, match="embedding failed"):
            provider.embed("feedback")


def test_embedding_service_persists_feedback_vector():
    feedback_id = uuid4()
    saved = {
        "embedding_id": uuid4(),
        "feedback_id": feedback_id,
        "analysis_id": uuid4(),
        "embedding_model": "test-model",
        "embedding_dimension": 3,
        "is_current": True,
        "created_at": datetime.now(timezone.utc),
    }
    provider = MagicMock()
    provider.model_name = "test-model"
    provider.embed.return_value = [0.1, 0.2, 0.3]
    with patch(
        "app.service.embedding_service.database.save_feedback_embedding",
        return_value=saved,
    ) as save:
        service = FeedbackEmbeddingService(provider=provider)
        result = service.embed_and_store({
            "feedback_id": feedback_id,
            "analysis_id": saved["analysis_id"],
            "feedback_text": "The card was charged twice.",
            "sentiment": "NEGATIVE",
            "category": "Payments",
            "theme": "Duplicate Debit",
            "emotion": "Frustrated",
            "priority": "HIGH",
            "assigned_department": "Payments Team",
            "short_summary": "Duplicate charge.",
        })

    assert result.feedbackId == feedback_id
    assert result.embeddingDimension == 3
    document = provider.embed.call_args.args[0]
    assert "The card was charged twice." in document
    assert "Duplicate Debit" in document
    save.assert_called_once_with(
        feedback_id=str(feedback_id),
        analysis_id=str(saved["analysis_id"]),
        embedding=[0.1, 0.2, 0.3],
        model_name="test-model",
    )


@patch("app.api.v1.endpoints.embeddings.FeedbackEmbeddingService")
def test_semantic_search_endpoint_returns_results(service_class):
    service = MagicMock()
    service.search.return_value = [{
        "feedbackId": str(uuid4()),
        "batchId": str(uuid4()),
        "feedbackText": "Card charged twice.",
        "sentiment": "NEGATIVE",
        "category": "Payments",
        "theme": "Duplicate Debit",
        "emotion": "Frustrated",
        "priority": "HIGH",
        "summary": "Duplicate charge.",
        "similarity": 0.92,
    }]
    service_class.return_value = service

    response = client.post(
        "/api/v1/semantic-search",
        json={"query": "duplicate card charge", "limit": 4},
    )

    assert response.status_code == 200
    assert response.json()[0]["similarity"] == 0.92
    service.search.assert_called_once_with("duplicate card charge", 4)


def test_semantic_search_endpoint_validates_limit():
    response = client.post(
        "/api/v1/semantic-search",
        json={"query": "duplicate card charge", "limit": 100},
    )
    assert response.status_code == 422


@patch("app.api.v1.endpoints.embeddings.EmbeddingWorkflow")
def test_single_embedding_endpoint_returns_not_found_for_missing_analysis(workflow_class):
    workflow = MagicMock()
    workflow.execute_single.side_effect = KeyError("missing feedback analysis")
    workflow_class.return_value = workflow

    response = client.post(f"/api/v1/embeddings/feedback/{uuid4()}")

    assert response.status_code == 404


@patch("app.workflow.embedding_workflow.database")
def test_batch_embedding_workflow_summarizes_successful_items(database_mock):
    database_mock.get_pending_embedding_records.return_value = [{
        "feedback_id": uuid4(),
        "batch_id": uuid4(),
        "analysis_id": uuid4(),
        "feedback_text": "The card was charged twice.",
        "sentiment": "NEGATIVE",
        "category": "Payments",
        "theme": "Duplicate Debit",
        "emotion": "Frustrated",
        "priority": "HIGH",
        "assigned_department": "Payments Team",
        "short_summary": "Duplicate charge.",
    }]
    service = MagicMock()
    workflow = EmbeddingWorkflow(service=service)
    workflow.settings.AI_RATE_LIMIT_DELAY = 0

    summary = workflow.execute_batch(str(uuid4()))

    assert summary.processed == 1
    assert summary.successful == 1
    assert summary.failed == 0
    service.embed_and_store.assert_called_once()


@patch("app.workflow.embedding_workflow.database")
def test_batch_embedding_workflow_records_item_failure(database_mock):
    record = {
        "feedback_id": uuid4(),
        "batch_id": uuid4(),
        "feedback_text": "The card was charged twice.",
    }
    database_mock.get_pending_embedding_records.return_value = [record]
    service = MagicMock()
    service.embed_and_store.side_effect = RuntimeError("provider unavailable")
    workflow = EmbeddingWorkflow(service=service)
    workflow.settings.AI_RATE_LIMIT_DELAY = 0

    summary = workflow.execute_batch(str(record["batch_id"]))

    assert summary.processed == 1
    assert summary.successful == 0
    assert summary.failed == 1
    database_mock.record_embedding_error.assert_called_once()
