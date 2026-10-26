from datetime import datetime, timezone
from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.core.config import Settings, get_settings

router = APIRouter()


class HealthResponse(BaseModel):
    status: str
    service: str
    environment: str
    version: str
    timestamp: str


@router.get("/health", response_model=HealthResponse, tags=["Health"])
def get_health(settings: Settings = Depends(get_settings)) -> HealthResponse:
    """
    Health check endpoint returning service status, environment, and version.
    """
    return HealthResponse(
        status="UP",
        service=settings.APP_NAME,
        environment=settings.ENVIRONMENT,
        version=settings.APP_VERSION,
        timestamp=datetime.now(timezone.utc).isoformat()
    )
