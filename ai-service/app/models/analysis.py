from datetime import datetime
from enum import Enum
from typing import Optional, Any, Dict
from uuid import UUID
from pydantic import BaseModel, Field, field_validator


class SentimentEnum(str, Enum):
    POSITIVE = "POSITIVE"
    NEUTRAL = "NEUTRAL"
    NEGATIVE = "NEGATIVE"


class PriorityEnum(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class FeedbackAnalysisDTO(BaseModel):
    """
    Strictly validated structured output schema for customer feedback analysis.
    Forced via JSON schema on LLM providers.
    """
    sentiment: SentimentEnum = Field(..., description="Overall sentiment of the feedback")
    category: str = Field(..., min_length=2, max_length=50, description="Functional business category")
    theme: str = Field(..., min_length=2, max_length=50, description="Specific issue theme or topic")
    emotion: str = Field(..., min_length=2, max_length=50, description="Primary customer emotion")
    priority: PriorityEnum = Field(..., description="Action priority level")
    assignedDepartment: str = Field(..., min_length=2, max_length=50, description="Target department responsible for resolution")
    summary: str = Field(..., max_length=200, description="Concise synopsis of the feedback, max 200 characters")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Confidence score from 0.0 to 1.0")

    @field_validator("sentiment", mode="before")
    @classmethod
    def normalize_sentiment(cls, v: Any) -> str:
        if isinstance(v, str):
            v_upper = v.strip().upper()
            if v_upper in SentimentEnum.__members__:
                return v_upper
        return v

    @field_validator("priority", mode="before")
    @classmethod
    def normalize_priority(cls, v: Any) -> str:
        if isinstance(v, str):
            v_upper = v.strip().upper()
            if v_upper in PriorityEnum.__members__:
                return v_upper
        return v

    @field_validator("summary", mode="before")
    @classmethod
    def enforce_summary_length(cls, v: Any) -> Any:
        if isinstance(v, str):
            v = v.strip()
            if len(v) > 200:
                return v[:197] + "..."
        return v


class AnalyzeFeedbackRequest(BaseModel):
    """
    Direct input payload to analyze feedback text without requiring database lookup.
    """
    feedbackId: Optional[str] = None
    feedbackText: str = Field(..., min_length=3, max_length=5000)
    rating: Optional[int] = Field(None, ge=1, le=5)
    channel: Optional[str] = None
    product: Optional[str] = None


class StoredAnalysisResponse(BaseModel):
    """
    Output model representing a persisted feedback analysis record.
    """
    analysisId: UUID
    feedbackId: UUID
    sentiment: SentimentEnum
    category: str
    theme: str
    emotion: str
    priority: PriorityEnum
    assignedDepartment: str
    summary: str
    confidence: float
    analyzedAt: datetime
    modelName: str
    promptVersion: str
    isCurrent: bool
    createdAt: datetime

    @classmethod
    def from_db_record(cls, record: Dict[str, Any]) -> "StoredAnalysisResponse":
        return cls(
            analysisId=record["analysis_id"],
            feedbackId=record["feedback_id"],
            sentiment=record["sentiment"],
            category=record["category"],
            theme=record["theme"],
            emotion=record["emotion"],
            priority=record["priority"],
            assignedDepartment=record["assigned_department"],
            summary=record["short_summary"],
            confidence=float(record["confidence_score"]),
            analyzedAt=record["analyzed_at"],
            modelName=record["model_name"],
            promptVersion=record["prompt_version"],
            isCurrent=record["is_current"],
            createdAt=record["created_at"],
        )


class BatchAnalysisSummaryResponse(BaseModel):
    """
    Execution summary produced after analyzing feedback records for a batch.
    """
    batchId: str
    processed: int
    successful: int
    failed: int
    durationSeconds: float
