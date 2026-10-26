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

## 5. Endpoints in Phase 1

- `GET /health`: General health check endpoint.
- `GET /internal/v1/health`: Blueprint-specified internal health check endpoint.
- `GET /docs`: Swagger OpenAPI documentation.
- `GET /redoc`: ReDoc API documentation.
