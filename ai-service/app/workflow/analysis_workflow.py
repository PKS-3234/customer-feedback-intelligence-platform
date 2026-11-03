import logging
import time
from typing import Optional, Dict, Any, List

from app.core import database
from app.core.config import get_settings
from app.models.analysis import (
    StoredAnalysisResponse,
    BatchAnalysisSummaryResponse,
)
from app.service.analysis_service import FeedbackAnalysisService

logger = logging.getLogger(__name__)


class AnalysisWorkflow:
    """
    Workflow orchestrator for feedback analysis execution.
    Executes single and batch feedback analysis workflows safely,
    managing rate limiting, error resilience, transaction isolation,
    and progress reporting.
    """

    def __init__(self, service: Optional[FeedbackAnalysisService] = None):
        self.service = service or FeedbackAnalysisService()
        self.settings = get_settings()

    def execute_single(self, feedback_id: str) -> StoredAnalysisResponse:
        """
        Executes single feedback analysis workflow.
        """
        return self.service.analyze_and_store_by_id(feedback_id)

    def execute_batch(
        self,
        batch_id: str,
        limit: Optional[int] = None,
    ) -> BatchAnalysisSummaryResponse:
        """
        Executes batch feedback analysis workflow for all AI_PENDING records in the specified batch.
        Iterates over pending records, applying configured rate limit delays and recording errors.
        """
        start_time = time.time()
        pending_records = database.get_pending_feedback_records(batch_id, limit=limit)
        total_pending = len(pending_records)
        logger.info(
            "Starting batch analysis for batch %s: found %d pending records.",
            batch_id,
            total_pending,
        )

        successful_count = 0
        failed_count = 0
        batch_size = self.settings.AI_BATCH_SIZE
        rate_delay = self.settings.AI_RATE_LIMIT_DELAY

        for index, record in enumerate(pending_records, start=1):
            feedback_id = str(record["feedback_id"])
            try:
                metadata = {
                    "rating": record.get("rating"),
                    "channel": record.get("channel"),
                    "product": record.get("product"),
                    "region": record.get("region"),
                    "customer_segment": record.get("customer_segment"),
                }

                # 1. Analyze text via provider
                dto = self.service.analyze_text(
                    feedback_text=record["feedback_text"],
                    metadata=metadata,
                )

                # 2. Persist analysis
                database.save_feedback_analysis(
                    feedback_id=feedback_id,
                    analysis_data=dto.model_dump(),
                    model_name=self.service.provider.get_model_name(),
                    prompt_version=self.service.provider.get_prompt_version(),
                )

                successful_count += 1

            except Exception as ex:
                failed_count += 1
                logger.error(
                    "Error analyzing feedback record %s in batch %s: %s",
                    feedback_id,
                    batch_id,
                    ex,
                )
                try:
                    database.record_analysis_error(
                        batch_id=batch_id,
                        feedback_id=feedback_id,
                        error_code="BATCH_ANALYSIS_ITEM_ERROR",
                        error_message=str(ex),
                        raw_row=record,
                    )
                    # Mark feedback status as FAILED so it does not block future runs
                    with database.get_db_connection() as conn:
                        with conn.cursor() as cur:
                            cur.execute(
                                """
                                UPDATE feedback
                                SET processing_status = 'FAILED', updated_at = CURRENT_TIMESTAMP
                                WHERE feedback_id = %s
                                """,
                                (feedback_id,),
                            )
                except Exception as db_err:
                    logger.error("Failed to mark feedback as failed: %s", db_err)

            # Apply safe rate-limiting delay between requests
            if rate_delay > 0 and index < total_pending:
                time.sleep(rate_delay)

            # Log periodic milestone progress
            if index % batch_size == 0 or index == total_pending:
                logger.info(
                    "Batch %s progress: %d/%d processed (%d successful, %d failed)",
                    batch_id,
                    index,
                    total_pending,
                    successful_count,
                    failed_count,
                )

        duration = round(time.time() - start_time, 2)
        logger.info(
            "Completed batch %s analysis in %.2fs: %d processed, %d successful, %d failed",
            batch_id,
            duration,
            total_pending,
            successful_count,
            failed_count,
        )

        return BatchAnalysisSummaryResponse(
            batchId=batch_id,
            processed=total_pending,
            successful=successful_count,
            failed=failed_count,
            durationSeconds=duration,
        )
