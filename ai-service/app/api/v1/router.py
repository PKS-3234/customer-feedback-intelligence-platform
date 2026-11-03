from fastapi import APIRouter
from app.api.v1.endpoints import health
from app.api.v1.endpoints import analysis
from app.api.v1.endpoints import embeddings
from app.api.v1.endpoints import agents

api_v1_router = APIRouter()

# Health endpoints
api_v1_router.include_router(health.router, prefix="", tags=["Health"])

# Analysis endpoints
api_v1_router.include_router(analysis.router, prefix="", tags=["Analysis"])

# Embedding generation and semantic search endpoints
api_v1_router.include_router(embeddings.router, prefix="", tags=["Embeddings"])

# Bounded multi-agent query workflow
api_v1_router.include_router(agents.router, prefix="", tags=["Agents"])
