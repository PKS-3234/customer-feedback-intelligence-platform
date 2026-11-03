from typing import Dict, Any, List, Optional
import logging

from app.core import database
from app.models.embeddings import (
    SemanticSearchResult,
    StoredEmbeddingResponse,
)
from app.provider.gemini_embeddings import GeminiEmbeddingProvider

logger = logging.getLogger(__name__)


class FeedbackEmbeddingService:
    def __init__(self, provider: Optional[GeminiEmbeddingProvider] = None):
        self.provider = provider or GeminiEmbeddingProvider()

    @staticmethod
    def _document_text(record: Dict[str, Any]) -> str:
        parts = [
            f"Customer feedback: {record['feedback_text']}",
            f"Sentiment: {record.get('sentiment') or 'Unknown'}",
            f"Category: {record.get('category') or 'Unknown'}",
            f"Theme: {record.get('theme') or 'Unknown'}",
            f"Emotion: {record.get('emotion') or 'Unknown'}",
            f"Priority: {record.get('priority') or 'Unknown'}",
            f"Department: {record.get('assigned_department') or 'Unknown'}",
        ]
        summary = record.get("short_summary")
        if summary:
            parts.append(f"Analysis summary: {summary}")
        return "\n".join(parts)

    def embed_and_store(
        self,
        record: Dict[str, Any],
    ) -> StoredEmbeddingResponse:
        vector = self.provider.embed(self._document_text(record))
        saved = database.save_feedback_embedding(
            feedback_id=str(record["feedback_id"]),
            analysis_id=str(record["analysis_id"]),
            embedding=vector,
            model_name=self.provider.model_name,
        )
        return StoredEmbeddingResponse(
            embeddingId=saved["embedding_id"],
            feedbackId=saved["feedback_id"],
            embeddingModel=saved["embedding_model"],
            embeddingDimension=saved["embedding_dimension"],
            isCurrent=saved["is_current"],
            createdAt=saved["created_at"],
        )

    def embed_feedback(self, feedback_id: str) -> StoredEmbeddingResponse:
        record = database.get_feedback_with_current_analysis(feedback_id)
        if not record:
            raise KeyError(
                f"Feedback record '{feedback_id}' with a current analysis was not found."
            )
        try:
            return self.embed_and_store(record)
        except Exception as ex:
            logger.error("Embedding generation failed for feedback %s.", feedback_id)
            database.record_embedding_error(
                batch_id=str(record["batch_id"]),
                feedback_id=feedback_id,
                error_code="EMBEDDING_FAILURE",
                error_message=str(ex),
                raw_row=record,
            )
            raise

    def get_embedding(self, feedback_id: str) -> Optional[StoredEmbeddingResponse]:
        record = database.get_current_feedback_embedding(feedback_id)
        if not record:
            return None
        return StoredEmbeddingResponse(
            embeddingId=record["embedding_id"],
            feedbackId=record["feedback_id"],
            embeddingModel=record["embedding_model"],
            embeddingDimension=record["embedding_dimension"],
            isCurrent=record["is_current"],
            createdAt=record["created_at"],
        )

    def search(self, query: str, limit: int) -> List[SemanticSearchResult]:
        vector = self.provider.embed(query, task_type="RETRIEVAL_QUERY")
        records = database.search_feedback_by_embedding(
            vector,
            limit,
            self.provider.model_name,
        )
        return [
            SemanticSearchResult(
                feedbackId=record["feedback_id"],
                batchId=record["batch_id"],
                feedbackText=record["feedback_text"],
                sentiment=record["sentiment"],
                category=record["category"],
                theme=record["theme"],
                emotion=record["emotion"],
                priority=record["priority"],
                summary=record["short_summary"],
                similarity=float(record["similarity"]),
            )
            for record in records
        ]
