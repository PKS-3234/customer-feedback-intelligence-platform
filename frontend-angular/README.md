# Customer Feedback Intelligence Platform - Frontend (Angular)

The web frontend for the Customer Feedback Intelligence Platform, built using Angular 21, TypeScript, and SCSS.

> The frontend supports CSV ingestion/monitoring and includes a batch AI analysis screen backed by Spring Boot.

---

## 1. Prerequisites

- Node.js 20+ (Installed: Node.js 24.15.0)
- npm 10+ (Installed: npm 11.12.1)
- Angular CLI 21+ (Installed: Angular CLI 21.2.9)

---

## 2. Installation & Setup (PowerShell on Windows)

```powershell
# Navigate to frontend-angular directory
cd D:\Projects\CustomerFeedbackIntelligence\frontend-angular

# Install dependencies (if setting up from fresh clone)
npm install
```

---

## 3. Configuration

Environment configurations are located in `src/environments/`:
- `src/environments/environment.development.ts` (Active during `ng serve` / development)
- `src/environments/environment.ts` (Active during production build)

Configuration keys:
- `apiBaseUrl`: Base endpoint for Spring Boot business API (`http://localhost:8080/api/v1`)
- `aiServiceBaseUrl`: Base endpoint for AI service (`http://localhost:8000`)
- `production`: Boolean flag indicating build environment

---

## 4. Development Server

```powershell
# Start Angular dev server on port 4200
npm start
# or: ng serve
```

Access the UI at: [http://localhost:4200](http://localhost:4200)

---

## 5. AI Analysis

Open `/analysis` (also available from the main navigation) to run AI analysis for pending feedback in a batch, optionally limit the number of records, and inspect persisted results. Existing results can be loaded by batch ID and individual feedback can be re-analyzed.

The UI calls Spring Boot endpoints under `/api/v1/analysis`; Spring Boot invokes the FastAPI service for analysis runs. The Gemini API key and model configuration belong to the AI service and are not exposed to the browser.

## 6. Testing

```powershell
# Run unit tests
npm test -- --watch=false
```

---

## 7. Production Build

```powershell
# Build optimized production bundle
npm run build
```
Bundle output is generated in `dist/frontend-angular`.
