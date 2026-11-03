from fastapi import APIRouter
from app.api.v1.endpoints import health
from app.api.v1.endpoints import analysis
from app.api.v1.endpoints import embeddings

api_v1_router = APIRouter()

# Health endpoints
api_v1_router.include_router(health.router, prefix="", tags=["Health"])

# Analysis endpoints
api_v1_router.include_router(analysis.router, prefix="", tags=["Analysis"])

# Embedding generation and semantic search endpoints
api_v1_router.include_router(embeddings.router, prefix="", tags=["Embeddings"])
