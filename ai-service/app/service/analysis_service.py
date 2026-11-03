import logging
from typing import Optional, Dict, Any

from app.core import database
from app.models.analysis import (
    FeedbackAnalysisDTO,
    StoredAnalysisResponse,
)
from app.provider.base import AnalysisProvider
from app.provider.factory import get_default_provider

logger = logging.getLogger(__name__)


class FeedbackAnalysisService:
    """
    Core business service for feedback analysis.
    Coordinates prompt construction, provider invocation, schema validation,
    and database lifecycle persistence.
    """

    def __init__(self, provider: Optional[AnalysisProvider] = None):
        self.provider = provider or get_default_provider()

    def analyze_text(
        self,
        feedback_text: str,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> FeedbackAnalysisDTO:
        """
        Validates feedback text input, invokes the analysis provider,
        and returns a validated FeedbackAnalysisDTO.
        """
        if not feedback_text or not feedback_text.strip():
            raise ValueError("Feedback text must not be empty.")

        dto = self.provider.analyze(
            feedback_text=feedback_text.strip(),
            metadata=metadata,
        )
        return dto

    def analyze_and_store_by_id(self, feedback_id: str) -> StoredAnalysisResponse:
        """
        1. Loads the raw feedback record from the database.
        2. Invokes provider to generate structured analysis.
        3. Persists results to feedback_analysis table with is_current=true.
        4. Updates feedback.processing_status to 'STORED'.
        5. Returns the stored record DTO.
        """
        record = database.get_feedback_record(feedback_id)
        if not record:
            raise KeyError(f"Feedback record with id '{feedback_id}' not found.")

        metadata = {
            "rating": record.get("rating"),
            "channel": record.get("channel"),
            "product": record.get("product"),
            "region": record.get("region"),
            "customer_segment": record.get("customer_segment"),
        }

        try:
            dto = self.analyze_text(
                feedback_text=record["feedback_text"],
                metadata=metadata,
            )
        except Exception as ex:
            logger.error("Failed to analyze feedback %s: %s", feedback_id, ex)
            database.record_analysis_error(
                batch_id=str(record["batch_id"]),
                feedback_id=feedback_id,
                error_code="ANALYSIS_FAILURE",
                error_message=str(ex),
                raw_row=record,
            )
            raise

        saved_row = database.save_feedback_analysis(
            feedback_id=feedback_id,
            analysis_data=dto.model_dump(),
            model_name=self.provider.get_model_name(),
            prompt_version=self.provider.get_prompt_version(),
        )

        return StoredAnalysisResponse.from_db_record(saved_row)

    def get_analysis_by_feedback_id(
        self, feedback_id: str
    ) -> Optional[StoredAnalysisResponse]:
        """
        Retrieves current stored analysis from the database.
        """
        row = database.get_latest_feedback_analysis(feedback_id)
        if not row:
            return None
        return StoredAnalysisResponse.from_db_record(row)
