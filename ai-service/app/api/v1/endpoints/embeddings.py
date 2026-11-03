import logging
from typing import List, Optional

from fastapi import APIRouter, HTTPException, Query, status

from app.models.embeddings import (
    BatchEmbeddingSummaryResponse,
    SemanticSearchRequest,
    SemanticSearchResult,
    StoredEmbeddingResponse,
)
from app.service.embedding_service import FeedbackEmbeddingService
from app.workflow.embedding_workflow import EmbeddingWorkflow

logger = logging.getLogger(__name__)

router = APIRouter(prefix="", tags=["Embeddings and Semantic Search"])


@router.get(
    "/embeddings/feedback/{feedbackId}",
    response_model=StoredEmbeddingResponse,
    summary="Get the current stored feedback embedding metadata",
)
def get_feedback_embedding(feedbackId: str) -> StoredEmbeddingResponse:
    record = FeedbackEmbeddingService().get_embedding(feedbackId)
    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"No current embedding found for feedback id '{feedbackId}'.",
        )
    return record


@router.post(
    "/embeddings/feedback/{feedbackId}",
    response_model=StoredEmbeddingResponse,
    summary="Generate and store a feedback embedding",
)
def embed_single_feedback(feedbackId: str) -> StoredEmbeddingResponse:
    try:
        return EmbeddingWorkflow().execute_single(feedbackId)
    except KeyError as ex:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(ex))
    except Exception as ex:
        logger.error("Single feedback embedding failed for %s.", feedbackId, exc_info=ex)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Embedding generation failed.",
        )


@router.post(
    "/embeddings/batches/{batchId}",
    response_model=BatchEmbeddingSummaryResponse,
    summary="Generate embeddings for feedback in a batch",
)
def embed_batch(
    batchId: str,
    limit: Optional[int] = Query(None, ge=1),
) -> BatchEmbeddingSummaryResponse:
    try:
        return EmbeddingWorkflow().execute_batch(batch_id=batchId, limit=limit)
    except Exception as ex:
        logger.error("Batch embedding failed for %s.", batchId, exc_info=ex)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Batch embedding failed.",
        )


@router.post(
    "/semantic-search",
    response_model=List[SemanticSearchResult],
    summary="Search feedback by semantic similarity",
)
def semantic_search(request: SemanticSearchRequest) -> List[SemanticSearchResult]:
    try:
        return FeedbackEmbeddingService().search(request.query, request.limit)
    except Exception as ex:
        logger.error("Semantic feedback search failed.", exc_info=ex)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Semantic search failed.",
        )
