# Customer Feedback Intelligence Platform - Spring Boot Backend

The business backend service for the Customer Feedback Intelligence Platform, built with Java 21 and Spring Boot 3.3.5.

> **Phase 1 Status**: Minimal skeleton with Spring Web, Actuator, Flyway, PostgreSQL driver, and JPA foundations. Health check endpoints and CORS are fully functional. Business entities, CSV ingestion, and AI integrations will be added in subsequent phases.

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
