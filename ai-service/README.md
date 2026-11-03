# AI Service (FastAPI)

FastAPI service for the Customer Feedback Intelligence Platform. Provides internal endpoints for feedback analysis, embeddings, semantic search, and supervisor agent execution.

> **Phase 1 Status**: Minimal skeleton with environment configuration, package routing, and health check endpoints. LangGraph, LangChain, and real model integrations will be introduced in subsequent phases.

---

## 1. Prerequisites

- Python 3.12+ (Installed: Python 3.12.10)
- `venv` module

---

## 2. Virtual Environment Setup (PowerShell on Windows)

```powershell
# Navigate to ai-service directory
cd D:\Projects\CustomerFeedbackIntelligence\ai-service

# Create virtual environment
python -m venv .venv

# Activate virtual environment
.\.venv\Scripts\Activate.ps1

# Install Phase 1 dependencies
pip install -r requirements.txt
```

---

## 3. Running Tests

```powershell
# Run pytest with verbose output
.\.venv\Scripts\pytest tests -v
```

---

## 4. Starting the Service

```powershell
# Start FastAPI service with hot reload on port 8000
.\.venv\Scripts\uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

---

## 5. Endpoints

- `GET /health`: General health check endpoint.
- `GET /internal/v1/health`: Blueprint-specified internal health check endpoint.
- `GET /docs`: Swagger OpenAPI documentation.
- `GET /redoc`: ReDoc API documentation.

Analysis endpoints are available under `/api/v1` and `/internal/v1`. Embedding and search endpoints include:

- `POST /api/v1/embeddings/feedback/{feedbackId}`: Generate and persist an embedding for feedback with a current analysis.
- `GET /api/v1/embeddings/feedback/{feedbackId}`: Retrieve current embedding metadata.
- `POST /api/v1/embeddings/batches/{batchId}?limit={count}`: Generate embeddings for analyzed feedback in a batch that does not have a current embedding for its current analysis.
- `POST /api/v1/semantic-search`: Generate a query embedding and search stored feedback by PGVector cosine similarity. Request JSON: `{"query":"duplicate card charge","limit":10}`.

Embedding documents include the raw feedback and its current structured analysis. Gemini `gemini-embedding-001` vectors use 768 dimensions and `RETRIEVAL_DOCUMENT` / `RETRIEVAL_QUERY` task types. Configure `EMBEDDING_PROVIDER`, `GEMINI_EMBEDDING_MODEL`, and `EMBEDDING_DIMENSIONS` in the environment. The embeddings table and cosine HNSW index are applied by the Spring Boot Flyway migration.
