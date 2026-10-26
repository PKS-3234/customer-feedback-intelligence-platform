# Customer Feedback Intelligence Platform

> **Agentic AI Customer Feedback Decision-Support Platform**  
> An enterprise-grade, evidence-backed platform that converts high-volume customer comments into structured, traceable business insights. Built using deterministic workflows for ingestion & processing, and agentic tool selection for analytical exploration.

---

## 1. Project Overview

Organizations receive feedback across diverse channels (reviews, surveys, support tickets, mobile applications). Manually triaging thousands of unstructured comments is inconsistent, slow, and unscalable.

The **Customer Feedback Intelligence Platform** provides:
- **Canonical Ingestion & Deduplication**: High-fidelity CSV intake, row validation, and immutable raw persistence.
- **Controlled AI Processing**: Deterministic LangGraph workflows ensuring schema-validated feedback classification, emotion detection, urgency scoring, and vector embeddings.
- **Semantic & Keyword Exploration**: Hybrid retrieval over PostgreSQL and PGVector to trace issues back to source comments.
- **Conversational Decision-Support Agent**: A supervisor agent that calls deterministic SQL tools and PGVector retrieval tools to answer questions with verified numbers and citations.

> [!NOTE]
> **Phase 1 Implementation Notice**: This repository currently contains the **Phase 1 Foundation Skeleton and Infrastructure**. CSV ingestion, JPA entities, LangGraph workflows, real model integrations, and dashboard interfaces will be introduced in subsequent phases as defined in the system blueprint.

---

## 2. Approved Architecture

The platform follows a strict separation of concerns:

```
+---------------------------------------------------------------------------------+
|                                 Angular 21 UI                                   |
|       (Executive Dashboard, Processing Monitor, Feedback Explorer, Agent Chat)  |
+---------------------------------------------------------------------------------+
                                      |  REST / JSON
                                      v
+---------------------------------------------------------------------------------+
|                       Spring Boot 3.3 Backend (Java 21)                         |
|   (Batch Ingestion, Validation, Relational Persistence, Business APIs, Security) |
+---------------------------------------------------------------------------------+
                                      |  REST / JSON
                                      v
+---------------------------------------------------------------------------------+
|                       FastAPI AI Service (Python 3.12)                          |
|    (LangGraph Processing Graph, PGVector Search, Supervisor Analytics Agent)    |
+---------------------------------------------------------------------------------+
                                      |
         +----------------------------+----------------------------+
         |                                                         |
         v                                                         v
+----------------------------------+             +----------------------------------+
|      PostgreSQL 16 + PGVector    |             |  Real LLM / Embedding Provider   |
| (Relational Data, Vector Storage)|             |     (Azure OpenAI / OpenAI)      |
+----------------------------------+             +----------------------------------+
```

### Responsibility Matrix

| Layer | Primary Responsibilities | Prohibited Boundaries |
|---|---|---|
| **Frontend (Angular)** | User experience, reactive filters, column mapping UI, chat interface | Direct database access, prompt construction, API keys, AI execution |
| **Business Backend (Spring Boot)** | File validation, batch management, raw persistence, query endpoints, DTO contracts | Free-form LLM prompts, LangGraph state execution |
| **AI Service (FastAPI)** | Deterministic LangGraph classification, embeddings, supervisor agent, analytical tools | Direct UI management, file upload multipart ownership |
| **Database (PostgreSQL + PGVector)** | Relational source of truth, vector indexing, audit trails, metrics queries | Application business logic, unconstrained writes |
| **Model Provider (Future)** | Semantic text representation, zero-shot taxonomy extraction | Direct database access, authoritative mathematical calculations |

---

## 3. Repository Structure

```
CustomerFeedbackIntelligence/
├── .env.example                     # Environment template with safe defaults
├── .gitignore                       # Multi-stack Git ignore rules
├── docker-compose.yml               # PostgreSQL 16 + PGVector container orchestration
├── README.md                        # Root documentation and developer guide
├── ai-service/                      # Python FastAPI AI Service
│   ├── app/
│   │   ├── __init__.py
│   │   ├── main.py                  # FastAPI application entry point
│   │   ├── api/
│   │   │   ├── __init__.py
│   │   │   └── v1/
│   │   │       ├── __init__.py
│   │   │       ├── router.py        # Internal API router
│   │   │       └── endpoints/
│   │   │           ├── __init__.py
│   │   │           └── health.py    # /health and /internal/v1/health endpoints
│   │   └── core/
│   │       ├── __init__.py
│   │       └── config.py            # Pydantic Settings configuration
│   ├── tests/
│   │   ├── __init__.py
│   │   └── test_health.py           # Pytest test suite for health endpoints
│   ├── requirements.txt             # Phase 1 pinned Python dependencies
│   └── README.md                    # AI service documentation
├── backend-spring/                  # Spring Boot Business Backend
│   ├── pom.xml                      # Maven project definition (Java 21, Spring Boot 3.3.5)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/customerfeedback/platform/
│   │   │   │   ├── CustomerFeedbackPlatformApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   └── CorsConfig.java          # Cross-Origin configuration
│   │   │   │   ├── controller/
│   │   │   │   │   └── HealthController.java    # /api/v1/health endpoint
│   │   │   │   └── dto/
│   │   │   │       └── HealthResponse.java      # Typed health DTO record
│   │   │   └── resources/
│   │   │       ├── application.yml              # Local environment configuration
│   │   │       └── db/migration/
│   │   │           └── V1__init_empty.sql       # Initial Flyway migration baseline
│   │   └── test/java/com/customerfeedback/platform/
│   │       ├── CustomerFeedbackPlatformApplicationTests.java
│   │       └── controller/
│   │           └── HealthControllerTest.java    # MockMvc endpoint tests
│   └── README.md                    # Spring Boot backend documentation
├── database/                        # Database initialization scripts
│   └── init/
│       └── 01-init-pgvector.sql     # PostgreSQL extension initialization (vector, uuid)
├── docs/                            # Architecture and product blueprints
│   └── Customer_Feedback_Intelligence_Platform_Blueprint.pdf
├── frontend-angular/                # Angular Web Frontend
│   ├── package.json                 # Angular 21 dependencies and scripts
│   ├── angular.json                 # Angular workspace configuration
│   ├── tsconfig.json                # TypeScript compiler configuration
│   ├── src/
│   │   ├── environments/
│   │   │   ├── environment.ts                   # Production environment settings
│   │   │   └── environment.development.ts       # Development environment settings
│   │   ├── app/
│   │   │   ├── app.ts               # Root component definition
│   │   │   ├── app.html             # Shell template with router-outlet
│   │   │   ├── app.routes.ts        # App route declarations
│   │   │   ├── app.spec.ts          # Root component unit tests
│   │   │   └── pages/home/
│   │   │       ├── home.component.ts            # Phase 1 landing dashboard
│   │   │       ├── home.component.html
│   │   │       ├── home.component.scss
│   │   │       └── home.component.spec.ts
│   │   └── main.ts                  # Angular bootstrap
│   └── README.md                    # Frontend documentation
└── sample-data/                     # Reference synthetic datasets
    └── banking_feedback_1000.csv    # 1,000 synthetic banking feedback records
```

---

## 4. Prerequisites

Verify installed toolchains before starting:

| Tool | Minimum Version | Verified Local Version |
|---|---|---|
| **OS** | Windows 11 / Linux / macOS | Windows 11 (amd64) |
| **Java** | 21 LTS | OpenJDK 21.0.12.1 LTS |
| **Maven** | 3.9+ | Apache Maven 3.10.0 |
| **Node.js** | 20+ | Node.js v24.15.0 |
| **npm** | 10+ | npm 11.12.1 |
| **Angular CLI** | 21+ | Angular CLI 21.2.9 |
| **Python** | 3.12+ | Python 3.12.10 |
| **Docker** | Docker Desktop 26+ | Docker 29.5.2 (Engine) |
| **Docker Compose** | v2.20+ | Docker Compose v5.1.3 |

---

## 5. Environment Setup

1. Copy the example environment template:
   ```powershell
   Copy-Item .env.example .env
   ```
2. The default variables in `.env.example` are preconfigured for local development with zero external dependencies:
   - Database name: `feedback_intel`
   - Database user/password: `postgres` / `postgres`
   - Database port: `5432`
   - Spring Boot port: `8080`
   - AI service port: `8000`
   - Angular dev port: `4200`

> [!CAUTION]
> Never commit `.env` or real API keys to version control.

---

## 6. Docker Database Startup (PostgreSQL + PGVector)

Start the PostgreSQL container with the PGVector extension enabled:

```powershell
# Start container in detached mode
docker compose up -d

# Verify container status and health
docker compose ps

# Verify PGVector and UUID extensions are active
docker exec cfip-postgres psql -U postgres -d feedback_intel -c "SELECT extname, extversion FROM pg_extension;"
```

Expected output:
```
  extname  | extversion 
-----------+------------
 plpgsql   | 1.0
 vector    | 0.8.7
 uuid-ossp | 1.1
```

---

## 7. Angular Installation and Startup

```powershell
# Navigate to the frontend directory
cd D:\Projects\CustomerFeedbackIntelligence\frontend-angular

# Install dependencies (only needed once or after updating package.json)
npm install

# Run unit tests
npm test -- --watch=false

# Build production bundle
npm run build

# Start local development server
npm start
```

Access the frontend application at: **[http://localhost:4200](http://localhost:4200)**

---

## 8. Spring Boot Build, Test, and Startup

```powershell
# Navigate to the backend-spring directory
cd D:\Projects\CustomerFeedbackIntelligence\backend-spring

# Run Maven tests (requires PostgreSQL container running on port 5432)
mvn clean test

# Build executable JAR
mvn clean package -DskipTests

# Run Spring Boot service
mvn spring-boot:run
# Alternatively:
# java -jar target/backend-spring-1.0.0-SNAPSHOT.jar
```

The Spring Boot backend will start on port `8080`.

---

## 9. FastAPI Virtual Environment, Installation, Test, and Startup

```powershell
# Navigate to the ai-service directory
cd D:\Projects\CustomerFeedbackIntelligence\ai-service

# Create virtual environment
python -m venv .venv

# Activate virtual environment
.\.venv\Scripts\Activate.ps1

# Install Phase 1 dependencies
pip install -r requirements.txt

# Run pytest suite
.\.venv\Scripts\pytest tests -v

# Start FastAPI development server
.\.venv\Scripts\uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The AI Service will start on port `8000`.

---

## 10. Health-Check URLs

| Service | Port | Endpoint URL | Description |
|---|---|---|---|
| **Spring Boot API** | `8080` | `http://localhost:8080/api/v1/health` | Documented application health status |
| **Spring Boot Actuator**| `8080` | `http://localhost:8080/actuator/health` | Detailed Spring Boot actuator metrics |
| **FastAPI Root** | `8000` | `http://localhost:8000/health` | Top-level service health check |
| **FastAPI Internal** | `8000` | `http://localhost:8000/internal/v1/health` | Blueprint-specified internal health check |
| **FastAPI Docs** | `8000` | `http://localhost:8000/docs` | Interactive Swagger API documentation |
| **Angular Web UI** | `4200` | `http://localhost:4200` | Frontend landing page & service status |
| **PostgreSQL** | `5432` | `localhost:5432/feedback_intel` | Tested via `pg_isready` healthcheck |

---

## 11. How to Stop Services

- **Angular Frontend**: Press `Ctrl + C` in the running PowerShell terminal.
- **Spring Boot Backend**: Press `Ctrl + C` in the running PowerShell terminal.
- **FastAPI AI Service**: Press `Ctrl + C` in the running PowerShell terminal.
- **PostgreSQL Database**:
  ```powershell
  # Stop container while preserving volume data
  docker compose down

  # Or stop and remove persistent volumes (fresh reset)
  # docker compose down -v
  ```

---

## 12. Connecting pgAdmin to PostgreSQL

To inspect the database using pgAdmin or any PostgreSQL GUI client (e.g. DBeaver):

1. **Host name/address**: `localhost` (or `127.0.0.1`)
2. **Port**: `5432`
3. **Maintenance database**: `feedback_intel`
4. **Username**: `postgres`
5. **Password**: `postgres`
6. Test connection: Verify access to schemas `public` and check that table `flyway_schema_history` exists.

---

## 13. Windows 11 & PowerShell Troubleshooting Guidance

1. **PowerShell Script Execution Policy**:
   - If running `.\.venv\Scripts\Activate.ps1` produces a script execution error:
     ```powershell
     Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
     ```
   - Alternatively, invoke executables directly without activating:
     ```powershell
     .\.venv\Scripts\python.exe -m pytest tests -v
     .\.venv\Scripts\uvicorn.exe app.main:app --host 0.0.0.0 --port 8000 --reload
     ```
2. **Port 5432 Already in Use**:
   - If Windows has a native PostgreSQL service running that occupies port 5432:
     ```powershell
     Get-Process -Id (Get-NetTCPConnection -LocalPort 5432).OwningProcess
     ```
   - Change `DB_PORT=5433` in `.env` and `docker-compose.yml` to remap the container port.
3. **Docker Desktop WSL 2 Engine Not Running**:
   - Ensure Docker Desktop is launched and the WSL 2 backend is running prior to executing `docker compose up -d`.
4. **Flyway / Database Connection Failure on Maven Test**:
   - Ensure the Docker PostgreSQL container is healthy before running `mvn test`:
     ```powershell
     docker compose ps
     ```

---

## 14. Phase 1 Scope Boundary Statement

> [!IMPORTANT]
> The following capabilities are **explicitly NOT part of Phase 1** and have **not been implemented**:
> - CSV ingestion, upload parsing, or column mapping.
> - JPA domain entities, repositories, or feedback tables.
> - Mock LLM implementations, OpenAI/Azure integrations, or paid API keys.
> - LangGraph workflows, LangChain chains, or agentic reasoning.
> - Embedding generation, vector cosine similarity queries, or RAG search.
> - Executive dashboards, charting libraries, or feedback explorers.
> - Authentication, security tokens, or cloud deployments.

---

## 15. Next Planned Phase: Phase 2

**Phase 2: Database Schema & CSV Ingestion**
- Implementation of Flyway migrations for `ingestion_batches`, `feedback`, `feedback_analysis`, `feedback_embeddings`, and `processing_errors`.
- Spring Boot CSV ingestion adapter with file preview, column mapping, row-level validation, and SHA-256 deduplication hashing.
- Upload CSV screen and Processing Monitor UI in Angular.
- Integration tests using synthetic dataset `sample-data/banking_feedback_1000.csv`.
