# AI Developer Codebase Copilot

A self-hosted AI codebase assistant. Connect a Git repository (GitHub clone or ZIP upload), let the backend index it (files, classes, methods, dependencies, semantic chunks), then explore it and run AI workflows: project Q&A with RAG sources, code explanation, architecture diagrams, bug hunting, code review, test generation, docs generation, and AI-assisted patch creation with a safe `git apply` approve/apply/reject flow.

- **Backend**: Java 17, Spring Boot 3.5, Spring AI 1.1 + Grok (xAI, OpenAI-compatible API), JGit, JavaParser, JPA/H2 or MySQL
- **Frontend**: React 19 + Vite 8, TypeScript, Tailwind CSS 4, Monaco editor, SSE streaming chat
- Extras: JWT auth (access + refresh), role-based access (USER / DEVELOPER / ADMIN), audit log, in-memory observability metrics

## Repository layout

```
backend/                 Spring Boot application (port 8080)
frontend/                React + Vite SPA (port 1234)
docker-compose.yml       MySQL + backend + frontend stack
.env.example             Environment variables template
```

## Quick start (local, no Docker)

Requirements: JDK 17+, Maven 3.9+ (or use any IDE), Node 24+.

### 1. Backend

```bash
cd backend
mvn package -DskipTests
java -jar target/codebase-copilot-backend-0.1.0.jar
```

Defaults to the `dev` profile with an in-memory H2 database (data resets on restart). Health check: `GET http://localhost:8080/actuator/health`.

To run against a local MySQL 8 instead (schema `codecopilot` is auto-created):

```bash
SPRING_PROFILES_ACTIVE=mysql DB_USERNAME=root DB_PASSWORD=your-password \
  java -jar target/codebase-copilot-backend-0.1.0.jar
```

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:1234. Vite proxies `/api` to `http://localhost:8080` (override with `VITE_BACKEND_URL`).

### 3. First run

1. Register a user at `/register` (first user is promoted to ADMIN; subsequent users get USER/DEVELOPER roles).
2. Create a project, then connect a repository via GitHub URL (`GITHUB_TOKEN` optional for public repos) or upload a ZIP.
3. Start indexing (`POST /api/repositories/{repoId}/index?projectId=...`). On the sample spring-petclinic repo this finishes in seconds (110 files, ~3000 chunks).
4. Explore: code, architecture, API endpoints, git history, search — then use the AI tools.

## AI configuration (Grok / xAI)

Set environment variables (or copy `.env.example`):

```bash
XAI_API_KEY=your_xai_key        # optional at runtime; REQUIRED for live AI answers
XAI_MODEL=grok-4                # default model
GITHUB_TOKEN=                   # optional, for private GitHub repos
JWT_SECRET=change-me            # production: use a long random string
```

Without `XAI_API_KEY` the app still runs: chat/agent/review/docs/patch actions fail fast with a clear "AI is not configured" message, chat streams the message over SSE, and indexing uses stub (hash) embeddings so search and architecture still work.

## Docker deployment

```bash
cp .env.example .env   # set XAI_API_KEY etc.
docker compose up -d --build
```

- `mysql` – MySQL 8 service (data persisted in a named volume)
- `backend` – runs with `SPRING_PROFILES_ACTIVE=mysql`, storage mounted at `/data/repos`
- `frontend` – production build served by nginx (port 1234, `/api` proxied to the backend container)

## API overview

| Area | Endpoints |
| --- | --- |
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `GET /api/auth/me` |
| Projects | `GET/POST /api/projects`, repos: `POST /api/projects/{id}/repositories` (GITHUB/ZIP), `GET /api/projects/{id}/repositories` |
| Indexing | `POST /api/repositories/{id}/index?projectId=...`, `GET /api/repositories/{id}/index/status?projectId=...` |
| Code | `GET /api/projects/{id}/files`, `/classes`, `/methods`, `/api-endpoints`, `/relationships` |
| Search | `GET /api/projects/{id}/search?query=...` (SYMBOL / SEMANTIC modes) |
| Git | `GET /api/projects/{id}/git/commits`, `GET /api/projects/{id}/git/diff` |
| Chat | `GET/POST /api/projects/{id}/chat/conversations`, `POST /api/projects/{id}/chat/messages` (sync), `POST /api/projects/{id}/chat/messages/stream` (SSE) |
| AI tools | `POST /api/projects/{id}/analyze` (code explanation), `/bugs`, `/reviews`, `POST /api/projects/{id}/tests`, `POST /api/projects/{id}/documentation`, `POST /api/projects/{id}/agent/run` |
| Agent/patch | `GET /api/agent/tools`, `POST /api/projects/{id}/patches` (generate), `POST /api/projects/{id}/patches/{pid}/approve` (apply via `git apply`) or `/reject` |
| Observability | `GET /api/admin/metrics` (ADMIN only), `GET /api/projects/{id}/audit` |

Responses are wrapped as `{ "success": true|false, "message": "...", "data": ..., "timestamp": "..." }`. SSE events: `event: token` / `event: sources` / `event: error`.

## Notes

- Indexed metadata and embeddings live in the database; cloned/uploaded repos live under `app.storage.root` (default `./data/repos`).
- Where-used queries (`/relationships`) rely on the JavaParser importer; external references resolve only when the same class is part of an indexed file.
- The audit log records AI tool calls and patch approvals per project. Metrics are in-memory counters, reset on restart.