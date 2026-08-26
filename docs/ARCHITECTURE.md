# HireHub Architecture

## Overview

HireHub is an AI-powered campus recruitment platform built with a clean separation between frontend and backend.

```
┌─────────────────────────────────────────────┐
│                Frontend                      │
│  React + TypeScript + Vite + Tailwind        │
│  Axios HTTP client                           │
│  JWT Bearer tokens + HttpOnly refresh cookie │
└─────────────────┬───────────────────────────┘
                  │ REST API (HTTPS)
┌─────────────────▼───────────────────────────┐
│           Spring Boot Backend                │
│  Spring Security (JWT + OAuth2)              │
│  Controllers → Services → Repositories       │
│  PostgreSQL + Flyway migrations              │
│  Resilience4j (circuit breaker + retry)      │
│  Micrometer metrics (Prometheus)             │
│  Structured logging (Logback)                │
├─────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ AI Service│  │ File     │  │ Auth     │  │
│  │ (OpenAI)  │  │ Storage  │  │ (JWT+O)  │  │
│  │ Resilience│  │ Local/S3 │  │ OAuth2   │  │
│  └──────────┘  └──────────┘  └──────────┘  │
└─────────────────────────────────────────────┘
```

## Technology Stack

### Frontend
- **React 19** + TypeScript
- **Vite 7** (bundler)
- **Tailwind CSS 4** + shadcn/ui components
- **Axios** (HTTP client with interceptors)
- **react-router 7** (routing)

### Backend
- **Java 21** + **Spring Boot 3.4.5**
- **Spring Security** (JWT + OAuth2)
- **Spring Data JPA** + **Hibernate**
- **PostgreSQL** + **Flyway** migrations
- **Resilience4j** (circuit breaker, retry)
- **Micrometer** + **Prometheus** (metrics)
- **Logback** (structured logging)
- **OpenAI** (AI features via WebClient)
- **Apache PDFBox** (PDF text extraction)
- **AWS SDK v2** (S3 file storage)

## Project Structure

### Frontend (`src/`)
```
src/
├── components/        # Reusable UI components
│   ├── dashboards/    # Role-specific dashboards
│   └── ui/            # shadcn/ui components
├── hooks/             # Custom React hooks
├── lib/               # Utilities, constants
├── pages/             # Route-level components
│   └── dashboard/     # Authenticated pages
├── services/          # API service modules
│   ├── api.ts         # Axios client with interceptors
│   ├── authService.ts
│   ├── jobService.ts
│   └── ...
└── index.css          # Tailwind + design tokens
```

### Backend (`backend/`)
```
backend/
├── src/main/java/com/hirehub/
│   ├── ai/              # AI service abstraction + OpenAI impl
│   │   ├── AiService.java              # Interface
│   │   ├── AiResilienceService.java    # Circuit breaker wrapper
│   │   ├── OpenAiService.java          # OpenAI implementation
│   │   ├── SkillMatchingService.java   # Deterministic matching
│   │   └── config/AiConfig.java
│   ├── aiinterview/     # AI mock interviews
│   ├── application/     # Job applications
│   ├── auth/            # Authentication (JWT + OAuth2)
│   ├── common/          # Shared DTOs, exceptions, enums
│   ├── config/          # Security, CORS, metrics, filters
│   ├── health/          # Health endpoint
│   ├── interview/       # Recruiter-scheduled interviews
│   ├── job/             # Job postings
│   ├── jobmatching/     # AI job matching
│   ├── notification/    # Notifications
│   ├── resume/          # Resume management + file storage
│   ├── resumeanalysis/  # AI resume analysis
│   ├── student/         # Student profiles
│   └── user/            # User accounts
├── src/main/resources/
│   ├── application.yml              # Base config
│   ├── application-dev.yml          # Development overrides
│   ├── application-prod.yml         # Production overrides
│   ├── logback-spring.xml           # Structured logging
│   └── db/migration/                # Flyway SQL migrations
├── src/test/                       # Integration tests
└── Dockerfile                      # Multi-stage build
```

## Authentication

### JWT Flow
1. User registers or logs in → receives access token (15 min) + refresh token (7 days, HttpOnly cookie)
2. Access token sent as `Authorization: Bearer <token>` header
3. Refresh token transmitted via cookie on `/api/v1/auth/refresh`
4. On 401, frontend attempts refresh → if fails, redirects to login

### OAuth2 Flow
1. User clicks "Continue with Google/GitHub"
2. Frontend obtains authorization code from provider
3. Code sent to `/api/v1/auth/oauth/google` or `/api/v1/auth/oauth/github`
4. Backend exchanges code, creates/links account, returns JWT tokens

### Authorization
- **STUDENT**: own profile, resumes, applications, AI features
- **RECRUITER**: own company, jobs, applicants to own jobs
- **ADMIN**: platform-wide access
- All authorization enforced server-side via `@PreAuthorize` + service-level ownership checks

## AI Features

### Resume Analysis
- PDF → text extraction (PDFBox) → OpenAI structured analysis
- 7-day cache: recent analysis reused for same resume
- Returns: score, skills, strengths, weaknesses, recommendations

### Job Matching
- Combines deterministic skill overlap (30%) with AI semantic analysis (70%)
- Per student + job + resume combination
- 7-day cache

### Mock Interviews
- Adaptive questioning based on previous answer quality
- Question categories: technical, behavioral, HR, project, resume
- Final report with category scores

### AI Resilience
- **Circuit breaker**: opens after 50% failure rate (10 calls), 30s wait
- **Retry**: 3 attempts with exponential backoff for transient failures
- **Graceful degradation**: clean 503 response when AI unavailable
- **Metrics**: request count, failure count, duration histograms

## Monitoring

### Actuator Endpoints
- `GET /actuator/health` — application + database health (authenticated)
- `GET /actuator/metrics` — all metrics
- `GET /actuator/prometheus` — Prometheus scrape endpoint

### Custom Metrics
- `hirehub.ai.resume.analysis` — resume analysis counter
- `hirehub.ai.job.matching` — job matching counter
- `hirehub.ai.mock.interview` — mock interview counter
- `hirehub.ai.failures` — AI failure counter
- `hirehub.auth.login` — login attempt counter
- `hirehub.auth.oauth` — OAuth attempt counter
- `hirehub.application.submitted` — application counter
- `hirehub.ai.request.duration` — AI request latency (p50, p95, p99)

### Structured Logging
- **Development**: human-readable console logs
- **Production**: JSON logs via LogstashEncoder with requestId, userId, method, URI, status, duration
- Sensitive data never logged: passwords, JWTs, API keys, resume contents

## Database

### Migrations
- Flyway manages schema via `V1__initial_schema.sql` through `V7__oauth_providers.sql`
- `spring.jpa.hibernate.ddl-auto=validate` in production

### Connection Pool (HikariCP)
- Max pool: 20, min idle: 5
- Connection timeout: 30s
- Idle timeout: 10 min
- Max lifetime: 30 min
- Leak detection: 60s threshold

## Docker

### Multi-stage Build
```
Stage 1: eclipse-temurin:21-jdk-alpine → Maven build
Stage 2: eclipse-temurin:21-jre-alpine → Runtime (non-root)
```

### Docker Compose
- PostgreSQL 16 + Spring Boot
- Health check dependency
- Persistent volume for database

## Error Contract

```json
{
  "timestamp": "2025-01-01T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/jobs",
  "requestId": "a1b2c3d4"
}
```

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `DB_URL` | PostgreSQL JDBC URL | Yes |
| `DB_USERNAME` | Database user | Yes |
| `DB_PASSWORD` | Database password | Yes |
| `JWT_SECRET` | JWT signing secret (32+ bytes) | Yes |
| `OPENAI_API_KEY` | OpenAI API key | For AI features |
| `OPENAI_MODEL` | AI model (default: gpt-4o) | No |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | For Google login |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret | For Google login |
| `GITHUB_CLIENT_ID` | GitHub OAuth client ID | For GitHub login |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth client secret | For GitHub login |
| `FILE_STORAGE_TYPE` | `local` or `s3` | No (default: local) |
| `S3_BUCKET` | AWS S3 bucket name | For S3 storage |
| `AWS_REGION` | AWS region | For S3 storage |
| `CORS_ORIGINS` | Comma-separated allowed origins | Yes |
| `SPRING_PROFILES_ACTIVE` | `dev`, `prod`, `test` | No (default: dev) |
