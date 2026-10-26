from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.endpoints.health import router as health_router
from app.api.v1.router import api_v1_router
from app.core.config import get_settings

settings = get_settings()

app = FastAPI(
    title="Customer Feedback Intelligence Platform - AI Service",
    description="FastAPI service for customer feedback processing and agentic analytics.",
    version=settings.APP_VERSION,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Top-level /health endpoint (Phase 1 requirement)
app.include_router(health_router, prefix="", tags=["Health"])

# Blueprint internal API prefix (/internal/v1/health)
app.include_router(api_v1_router, prefix="/internal/v1")


@app.get("/", tags=["Root"])
def root():
    return {
        "service": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "status": "UP",
        "docs": "/docs",
        "health": "/health",
        "internal_health": "/internal/v1/health",
    }
