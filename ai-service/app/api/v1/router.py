from fastapi import APIRouter
from app.api.v1.endpoints import health
from app.api.v1.endpoints import analysis

api_v1_router = APIRouter()

# Health endpoints
api_v1_router.include_router(health.router, prefix="", tags=["Health"])

# Analysis endpoints
api_v1_router.include_router(analysis.router, prefix="", tags=["Analysis"])
