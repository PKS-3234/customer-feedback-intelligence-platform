# Customer Feedback Intelligence Platform - Spring Boot Backend

The business backend service for the Customer Feedback Intelligence Platform, built with Java 21 and Spring Boot 3.3.5.

> The backend includes CSV ingestion and a Spring-to-FastAPI integration for triggering AI analysis and retrieving persisted results.

---

## 1. Prerequisites

- Java 21 (Installed: OpenJDK 21.0.12.1)
- Apache Maven 3.9+ (Installed: Maven 3.10.0)
- PostgreSQL with PGVector running (via root `docker compose up -d`)

---

## 2. Configuration

Configuration is managed via `src/main/resources/application.yml` and can be overridden using environment variables:

| Environment Variable | Default Value | Purpose |
|----------------------|---------------|---------|
| `PORT` | `8080` | Spring Boot HTTP port |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `feedback_intel` | PostgreSQL database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Allowed frontend origins |
| `AI_SERVICE_BASE_URL` | `http://localhost:8000` | Internal FastAPI service URL |

---

## 3. Build & Run Commands (PowerShell on Windows)

```powershell
# Navigate to backend-spring directory
cd D:\Projects\CustomerFeedbackIntelligence\backend-spring

# Run tests
mvn clean test

# Package executable JAR
mvn clean package -DskipTests

# Run application via Maven
mvn spring-boot:run

# Or run the packaged JAR directly
java -jar target/backend-spring-1.0.0-SNAPSHOT.jar
```

---

## 4. Health Endpoints

- `GET http://localhost:8080/api/v1/health`: Documented API health endpoint returning:
  ```json
  {
    "status": "UP",
    "service": "customer-feedback-platform-backend",
    "version": "1.0.0",
    "timestamp": "2026-10-26T08:06:56.123Z"
  }
  ```
- `GET http://localhost:8080/actuator/health`: Spring Boot Actuator health endpoint.

## 5. AI Analysis Endpoints

Spring proxies analysis requests to the FastAPI service configured by `AI_SERVICE_BASE_URL`. FastAPI/Gemini analysis results are persisted in the shared PostgreSQL database; Spring exposes result retrieval from `feedback_analysis`.

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/v1/analysis/feedback/{feedbackId}` | Analyze and persist one feedback record |
| `POST` | `/api/v1/analysis/batches/{batchId}?limit={count}` | Analyze pending feedback in a batch; `limit` is optional and must be positive |
| `GET` | `/api/v1/analysis/feedback/{feedbackId}` | Retrieve the current persisted analysis for feedback |
| `GET` | `/api/v1/analysis/batches/{batchId}` | List current persisted analyses for a batch |

Analysis run requests return `502 Bad Gateway` when the AI service is unavailable. A missing upstream feedback record returns `404`.

## 6. Embedding and Semantic Search Endpoints

The AI service generates 768-dimensional Gemini embeddings for feedback with current analysis, persisting versioned vectors to `feedback_embeddings`. Spring proxies generation and semantic search requests; search results include current analysis metadata and a cosine similarity score.

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/v1/embeddings/feedback/{feedbackId}` | Generate and persist a feedback embedding |
| `GET` | `/api/v1/embeddings/feedback/{feedbackId}` | Retrieve current embedding metadata |
| `POST` | `/api/v1/embeddings/batches/{batchId}?limit={count}` | Generate embeddings for analyzed feedback not yet embedded for its current analysis |
| `POST` | `/api/v1/semantic-search` | Semantic search; JSON body: `{"query":"duplicate card charge","limit":10}` |

Flyway migration V3 fixes the vector dimension, links each embedding to the analysis version it represents, and adds a cosine HNSW index for current embeddings.
