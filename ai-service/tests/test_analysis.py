from unittest.mock import MagicMock, patch
import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.models.analysis import (
    FeedbackAnalysisDTO,
    SentimentEnum,
    PriorityEnum,
    AnalyzeFeedbackRequest,
)
from app.provider.base import AnalysisProvider
from app.provider.gemini import GeminiAnalysisProvider
from app.service.analysis_service import FeedbackAnalysisService
from app.workflow.analysis_workflow import AnalysisWorkflow

client = TestClient(app)


# ---------------------------------------------------------------------------
# 1. Pydantic Model Validation Tests
# ---------------------------------------------------------------------------

def test_feedback_analysis_dto_validation_success():
    data = {
        "sentiment": "NEGATIVE",
        "category": "Payments",
        "theme": "Duplicate Debit",
        "emotion": "Frustrated",
        "priority": "HIGH",
        "assignedDepartment": "Payments Team",
        "summary": "Customer charged twice for single grocery purchase.",
        "confidence": 0.95,
    }
    dto = FeedbackAnalysisDTO.model_validate(data)
    assert dto.sentiment == SentimentEnum.NEGATIVE
    assert dto.priority == PriorityEnum.HIGH
    assert dto.confidence == 0.95
    assert "charged twice" in dto.summary


def test_feedback_analysis_dto_case_insensitive_enums():
    data = {
        "sentiment": "positive",
        "category": "Mobile App",
        "theme": "App Usability",
        "emotion": "Satisfied",
        "priority": "low",
        "assignedDepartment": "Mobile Engineering",
        "summary": "Great update and smooth navigation.",
        "confidence": 0.88,
    }
    dto = FeedbackAnalysisDTO.model_validate(data)
    assert dto.sentiment == SentimentEnum.POSITIVE
    assert dto.priority == PriorityEnum.LOW


def test_feedback_analysis_dto_summary_trimming():
    long_summary = "A" * 250
    data = {
        "sentiment": "NEUTRAL",
        "category": "General",
        "theme": "Inquiry",
        "emotion": "Confused",
        "priority": "MEDIUM",
        "assignedDepartment": "Support Team",
        "summary": long_summary,
        "confidence": 0.75,
    }
    dto = FeedbackAnalysisDTO.model_validate(data)
    assert len(dto.summary) <= 200
    assert dto.summary.endswith("...")


def test_feedback_analysis_dto_confidence_out_of_bounds():
    data = {
        "sentiment": "POSITIVE",
        "category": "General",
        "theme": "General",
        "emotion": "Happy",
        "priority": "LOW",
        "assignedDepartment": "Support Team",
        "summary": "Great",
        "confidence": 1.5,
    }
    with pytest.raises(Exception):
        FeedbackAnalysisDTO.model_validate(data)


# ---------------------------------------------------------------------------
# 2. Mock Provider & Service Tests
# ---------------------------------------------------------------------------

class MockAnalysisProvider(AnalysisProvider):
    def __init__(self, should_fail: bool = False):
        self.should_fail = should_fail

    def get_model_name(self) -> str:
        return "mock-gemini-test"

    def get_prompt_version(self) -> str:
        return "v1.0-test"

    def analyze(self, feedback_text: str, metadata=None) -> FeedbackAnalysisDTO:
        if self.should_fail:
            raise RuntimeError("Simulated provider failure")
        return FeedbackAnalysisDTO(
            sentiment=SentimentEnum.NEGATIVE,
            category="Mobile App",
            theme="App Crash",
            emotion="Frustrated",
            priority=PriorityEnum.HIGH,
            assignedDepartment="Mobile Engineering",
            summary=f"Analysis of: {feedback_text[:50]}",
            confidence=0.92,
        )


def test_feedback_analysis_service_text_analysis():
    provider = MockAnalysisProvider()
    service = FeedbackAnalysisService(provider=provider)

    dto = service.analyze_text("App keeps crashing when I view statements.")
    assert dto.sentiment == SentimentEnum.NEGATIVE
    assert dto.category == "Mobile App"
    assert dto.theme == "App Crash"
    assert dto.confidence == 0.92


def test_feedback_analysis_service_empty_text_raises():
    provider = MockAnalysisProvider()
    service = FeedbackAnalysisService(provider=provider)

    with pytest.raises(ValueError, match="must not be empty"):
        service.analyze_text("   ")


def test_feedback_analysis_service_provider_error():
    provider = MockAnalysisProvider(should_fail=True)
    service = FeedbackAnalysisService(provider=provider)

    with pytest.raises(RuntimeError, match="Simulated provider failure"):
        service.analyze_text("Valid text")


# ---------------------------------------------------------------------------
# 3. Gemini Provider Retry Behavior Test
# ---------------------------------------------------------------------------

@patch("httpx.Client.post")
def test_gemini_provider_retry_on_transient_error(mock_post):
    # Simulate: 1st call fails with 503, 2nd call succeeds with valid JSON
    mock_resp_fail = MagicMock()
    mock_resp_fail.status_code = 503
    mock_resp_fail.raise_for_status.side_effect = Exception("503 Service Unavailable")

    mock_resp_success = MagicMock()
    mock_resp_success.status_code = 200
    mock_resp_success.json.return_value = {
        "candidates": [
            {
                "content": {
                    "parts": [
                        {
                            "text": '{"sentiment": "POSITIVE", "category": "Support", "theme": "Fast Response", "emotion": "Satisfied", "priority": "LOW", "assignedDepartment": "Support Team", "summary": "Great agent help.", "confidence": 0.98}'
                        }
                    ]
                }
            }
        ]
    }
    mock_post.side_effect = [mock_resp_fail, mock_resp_success]

    provider = GeminiAnalysisProvider(api_key="fake-key-for-test", retries=2)
    dto = provider.analyze("Thank you for the quick support!")

    assert dto.sentiment == SentimentEnum.POSITIVE
    assert dto.confidence == 0.98
    assert mock_post.call_count == 2


# ---------------------------------------------------------------------------
# 4. API Endpoint Integration Tests
# ---------------------------------------------------------------------------

@patch("app.api.v1.endpoints.analysis.FeedbackAnalysisService")
def test_api_direct_analyze_endpoint(mock_service_cls):
    mock_service = MagicMock()
    mock_service.analyze_text.return_value = FeedbackAnalysisDTO(
        sentiment=SentimentEnum.NEGATIVE,
        category="Loans",
        theme="Refund Delay",
        emotion="Angry",
        priority=PriorityEnum.CRITICAL,
        assignedDepartment="Operations Team",
        summary="Refund still pending after 3 weeks.",
        confidence=0.96,
    )
    mock_service_cls.return_value = mock_service

    resp = client.post(
        "/api/v1/analyze",
        json={"feedbackText": "Refund has not been credited for over 3 weeks!"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["sentiment"] == "NEGATIVE"
    assert body["category"] == "Loans"
    assert body["priority"] == "CRITICAL"
    assert body["confidence"] == 0.96


def test_api_analyze_missing_feedback_id():
    resp = client.post("/api/v1/analyze/00000000-0000-0000-0000-000000000000")
    # Should return 404 because record doesn't exist
    assert resp.status_code == 404


def test_api_get_analysis_missing_feedback_id():
    resp = client.get("/api/v1/analyze/00000000-0000-0000-0000-000000000000")
    assert resp.status_code == 404


@patch("app.api.v1.endpoints.analysis.AnalysisWorkflow")
def test_api_batch_analyze_endpoint(mock_workflow_cls):
    from app.models.analysis import BatchAnalysisSummaryResponse

    mock_wf = MagicMock()
    mock_wf.execute_batch.return_value = BatchAnalysisSummaryResponse(
        batchId="batch-1234",
        processed=10,
        successful=9,
        failed=1,
        durationSeconds=2.5,
    )
    mock_workflow_cls.return_value = mock_wf

    resp = client.post("/api/v1/analyze/batch/batch-1234?limit=10")
    assert resp.status_code == 200
    body = resp.json()
    assert body["batchId"] == "batch-1234"
    assert body["processed"] == 10
    assert body["successful"] == 9
    assert body["failed"] == 1
