import logging
import time
from typing import Optional

from app.core import database
from app.core.config import get_settings
from app.models.embeddings import BatchEmbeddingSummaryResponse
from app.service.embedding_service import FeedbackEmbeddingService

logger = logging.getLogger(__name__)


class EmbeddingWorkflow:
    def __init__(self, service: Optional[FeedbackEmbeddingService] = None):
        self.service = service or FeedbackEmbeddingService()
        self.settings = get_settings()

    def execute_single(self, feedback_id: str):
        return self.service.embed_feedback(feedback_id)

    def execute_batch(
        self,
        batch_id: str,
        limit: Optional[int] = None,
    ) -> BatchEmbeddingSummaryResponse:
        if limit is not None and limit < 1:
            raise ValueError("Embedding limit must be greater than zero.")

        started = time.time()
        records = database.get_pending_embedding_records(
            batch_id,
            model_name=self.service.provider.model_name,
            limit=limit,
        )
        successful = 0
        failed = 0

        for index, record in enumerate(records, start=1):
            feedback_id = str(record["feedback_id"])
            try:
                self.service.embed_and_store(record)
                successful += 1
            except Exception as ex:
                failed += 1
                logger.error(
                    "Embedding generation failed for feedback %s in batch %s.",
                    feedback_id,
                    batch_id,
                    exc_info=ex,
                )
                database.record_embedding_error(
                    batch_id=batch_id,
                    feedback_id=feedback_id,
                    error_code="EMBEDDING_FAILURE",
                    error_message=str(ex),
                    raw_row=record,
                )

            delay = self.settings.AI_RATE_LIMIT_DELAY
            if delay > 0 and index < len(records):
                time.sleep(delay)

        return BatchEmbeddingSummaryResponse(
            batchId=batch_id,
            processed=len(records),
            successful=successful,
            failed=failed,
            durationSeconds=round(time.time() - started, 2),
        )
