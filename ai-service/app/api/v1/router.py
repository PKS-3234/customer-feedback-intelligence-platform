from fastapi import APIRouter
from app.api.v1.endpoints import health

api_v1_router = APIRouter()

# Internal API routes per blueprint specification (Section 9.2: GET /internal/v1/health)
api_v1_router.include_router(health.router, prefix="", tags=["Internal"])
