from typing import Optional
from fastapi import APIRouter, HTTPException, Query, status
import logging

from app.models.analysis import (
    AnalyzeFeedbackRequest,
    FeedbackAnalysisDTO,
    StoredAnalysisResponse,
    BatchAnalysisSummaryResponse,
)
from app.service.analysis_service import FeedbackAnalysisService
from app.workflow.analysis_workflow import AnalysisWorkflow

logger = logging.getLogger(__name__)

router = APIRouter(prefix="", tags=["Analysis"])


@router.post(
    "/analyze",
    response_model=FeedbackAnalysisDTO,
    summary="Analyze feedback text directly",
    description="Analyzes feedback comment text using Gemini structured JSON schema output without database persistence.",
)
def analyze_direct(request: AnalyzeFeedbackRequest) -> FeedbackAnalysisDTO:
    service = FeedbackAnalysisService()
    try:
        metadata = {}
        if request.rating is not None:
            metadata["rating"] = request.rating
        if request.channel:
            metadata["channel"] = request.channel
        if request.product:
            metadata["product"] = request.product

        return service.analyze_text(
            feedback_text=request.feedbackText,
            metadata=metadata if metadata else None,
        )
    except ValueError as ve:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(ve))
    except Exception as ex:
        logger.error("Direct analysis failed: %s", ex)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Analysis engine failure: {str(ex)}",
        )


@router.post(
    "/analyze/{feedbackId}",
    response_model=StoredAnalysisResponse,
    summary="Analyze and persist single feedback record",
    description="Loads raw feedback by ID from the database, executes Gemini analysis, stores results into feedback_analysis, and updates status.",
)
def analyze_single_feedback(feedbackId: str) -> StoredAnalysisResponse:
    workflow = AnalysisWorkflow()
    try:
        return workflow.execute_single(feedbackId)
    except KeyError as ke:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(ke))
    except ValueError as ve:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(ve))
    except Exception as ex:
        logger.error("Feedback analysis failed for %s: %s", feedbackId, ex)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Analysis pipeline error: {str(ex)}",
        )


@router.post(
    "/analyze/batch/{batchId}",
    response_model=BatchAnalysisSummaryResponse,
    summary="Batch analyze feedback records",
    description="Iterates over all AI_PENDING records in the specified batch, executes Gemini analysis, stores records, and reports progress summary.",
)
def analyze_batch(
    batchId: str,
    limit: Optional[int] = Query(None, description="Optional limit of records to analyze in this execution run"),
) -> BatchAnalysisSummaryResponse:
    workflow = AnalysisWorkflow()
    try:
        return workflow.execute_batch(batch_id=batchId, limit=limit)
    except Exception as ex:
        logger.error("Batch analysis execution failed for %s: %s", batchId, ex)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Batch execution failure: {str(ex)}",
        )


@router.get(
    "/analyze/{feedbackId}",
    response_model=StoredAnalysisResponse,
    summary="Get stored feedback analysis",
    description="Retrieves the current active analysis record for the given feedback ID.",
)
def get_analysis_by_feedback_id(feedbackId: str) -> StoredAnalysisResponse:
    service = FeedbackAnalysisService()
    record = service.get_analysis_by_feedback_id(feedbackId)
    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"No active analysis found for feedback id '{feedbackId}'.",
        )
    return record
