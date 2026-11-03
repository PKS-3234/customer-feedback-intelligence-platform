from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, Field


class StoredEmbeddingResponse(BaseModel):
    embeddingId: UUID
    feedbackId: UUID
    embeddingModel: str
    embeddingDimension: int
    isCurrent: bool
    createdAt: datetime


class BatchEmbeddingSummaryResponse(BaseModel):
    batchId: str
    processed: int
    successful: int
    failed: int
    durationSeconds: float


class SemanticSearchRequest(BaseModel):
    query: str = Field(..., min_length=2, max_length=2000)
    limit: int = Field(default=10, ge=1, le=50)


class SemanticSearchResult(BaseModel):
    feedbackId: UUID
    batchId: UUID
    feedbackText: str
    sentiment: Optional[str] = None
    category: Optional[str] = None
    theme: Optional[str] = None
    emotion: Optional[str] = None
    priority: Optional[str] = None
    summary: Optional[str] = None
    similarity: float = Field(..., ge=0.0, le=1.0)
