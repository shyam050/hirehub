# HireHub

A full-stack campus recruitment platform where students find and apply for jobs, recruiters manage candidates, and admins oversee the entire placement process.

Built with **React**, **TypeScript**, **Tailwind CSS**, and **Spring Boot** with **PostgreSQL**.

---

## Features

### Student
- Register and create a profile (education, skills, projects)
- Upload and manage PDF resumes
- Browse, search, and filter job positions
- Apply for jobs with a single click
- Track application status through a visual timeline
- View and manage upcoming interviews
- Receive notifications for status changes and interview updates
- AI resume analysis, job matching, and mock interview coaching

### Recruiter
- Register and create a company profile
- Post, edit, and close job positions
- Review and filter applicants
- Shortlist, reject, or advance candidates through stages
- Schedule, reschedule, cancel, and complete interviews
- Submit interview feedback
- Receive notifications for new applications

### Admin
- Manage students, recruiters, and companies
- Approve or reject company registrations
- View all jobs, applications, and platform statistics

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, TypeScript, React Router, Vite |
| Styling | Tailwind CSS, shadcn/ui |
| Backend | Spring Boot 3.x, Java 21, Maven |
| Database | PostgreSQL (Flyway migrations) |
| Auth | JWT (access + HttpOnly refresh cookies), OAuth2 (Google, GitHub) |
| AI | OpenAI (server-side, via Spring WebFlux) |
| File Storage | Local (dev) / AWS S3 (prod) |
| API Docs | Swagger UI (springdoc-openapi) |
| Monitoring | Spring Boot Actuator |
| Containerization | Docker + Docker Compose |
| CI | GitHub Actions |
| Rate Limiting | In-memory sliding window |

---

## Project Structure

```
├── src/                       # React frontend
│   ├── components/            # Shared UI components
│   │   ├── ui/                # shadcn/ui primitives
│   │   ├── dashboards/        # Role-specific dashboard views
│   │   ├── DashboardSidebar.tsx
│   │   ├── Notifications.tsx
│   │   └── ScheduleInterviewDialog.tsx
│   ├── pages/                 # Route pages
│   │   ├── Landing.tsx
│   │   ├── Auth.tsx
│   │   ├── Dashboard.tsx
│   │   └── dashboard/         # Dashboard sub-pages
│   ├── services/              # API service modules
│   │   ├── authService.ts
│   │   ├── jobService.ts
│   │   ├── applicationService.ts
│   │   ├── resumeService.ts
│   │   ├── interviewService.ts
│   │   ├── notificationService.ts
│   │   └── aiService.ts
│   ├── lib/                   # Constants, API client, utilities
│   ├── hooks/                 # Auth hooks
│   ├── main.tsx               # App entry + routing
│   └── index.css              # Theme tokens + styles
├── backend/                   # Spring Boot backend
│   ├── src/main/java/com/hirehub/
│   │   ├── config/            # CORS, Security, JWT
│   │   ├── auth/              # Auth controller, service, JWT, OAuth
│   │   ├── user/              # User controller, service
│   │   ├── student/           # Student profile
│   │   ├── recruiter/         # Recruiter profile
│   │   ├── company/           # Company management
│   │   ├── job/               # Job CRUD
│   │   ├── application/       # Application management
│   │   ├── resume/            # Resume upload, storage, analysis
│   │   ├── interview/         # Interview scheduling
│   │   ├── notification/      # Notifications
│   │   ├── ai/                # AI service abstraction, OpenAI
│   │   ├── resumeanalysis/    # AI resume analysis
│   │   ├── jobmatching/       # AI job matching
│   │   └── aiinterview/       # AI mock interviews
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/      # Flyway SQL migrations
├── package.json
├── tsconfig.json
└── vite.config.ts
```

---

## Prerequisites

- **Node.js** v18 or later
- **Bun** (recommended) or npm/yarn
- **Java 21** (for backend)
- **Maven** (for backend)
- **PostgreSQL** (for backend)

---

## Setup Instructions

### 1. Clone the project

```bash
git clone <your-repo-url>
cd hirehub
```

Or if you downloaded the ZIP:

```bash
unzip hirehub.zip
cd hirehub
```

### 2. Install frontend dependencies

```bash
bun install
```

### 3. Set up PostgreSQL

Create a PostgreSQL database:

```sql
CREATE DATABASE hirehub_dev;
CREATE USER hirehub WITH PASSWORD 'hirehub_dev';
GRANT ALL PRIVILEGES ON DATABASE hirehub_dev TO hirehub;
```

Flyway will create all tables automatically on first startup.

### 4. Configure environment variables

Frontend (`VITE_API_URL`):

```
VITE_API_URL=http://localhost:8080/api/v1
```

Backend (set in your environment or IDE):

```
DB_URL=jdbc:postgresql://localhost:5432/hirehub_dev
DB_USERNAME=hirehub
DB_PASSWORD=hirehub_dev
JWT_SECRET=<base64-encoded-secret>
OPENAI_API_KEY=<your-openai-key>        # optional, for AI features
GOOGLE_CLIENT_ID=<your-google-id>       # optional, for OAuth
GOOGLE_CLIENT_SECRET=<your-google-secret>
GITHUB_CLIENT_ID=<your-github-id>       # optional, for OAuth
GITHUB_CLIENT_SECRET=<your-github-secret>
```

### 5. Start the backend

```bash
cd backend
mvn spring-boot:run
```

The backend starts at `http://localhost:8080`.

### 6. Start the frontend

```bash
# From project root
bun run dev
```

The frontend starts at `http://localhost:5173`.

---

## First-Time Usage

1. **Sign up** with your email or Google/GitHub
2. **Select your role** — Student or Recruiter
3. **Complete your profile**
4. Start using the platform!

> **Admin accounts** cannot be self-assigned. They must be created directly in the database or through a separate admin provisioning process.

---

## Database Tables

The database uses Flyway migrations (V1–V7):

| Table | Purpose |
|-------|---------|
| `users` | Authentication and role management |
| `students` | Student profiles (education, skills, projects) |
| `recruiters` | Recruiter profiles linked to companies |
| `companies` | Company information and approval status |
| `jobs` | Job postings with requirements and metadata |
| `applications` | Job applications with status and timeline |
| `resumes` | PDF resume storage references |
| `interviews` | Interview scheduling, status, and feedback |
| `notifications` | In-app notification system |
| `refresh_tokens` | Server-side refresh token storage |
| `resume_analyses` | AI resume analysis results |
| `job_matches` | AI job matching results |
| `ai_interviews` | AI mock interview sessions |
| `ai_interview_questions` | AI interview questions + evaluations |
| `oauth_providers` | OAuth provider links (Google, GitHub) |

---

## Application Flow

### Student
```
Register → Complete Profile → Upload Resume → Browse Jobs → Apply → Track Application → Interview → Feedback
```

### Recruiter
```
Register → Create Company → (Admin Approval) → Post Positions → Review Applicants → Schedule Interviews → Submit Feedback
```

### Application Stages
```
Applied → Screening → Shortlisted → Technical Interview → HR Interview → Offered → Selected / Rejected
```

Every status change is recorded in the application timeline.

---

## Authorization

Every API endpoint enforces authorization server-side:

- **Students** can only access their own profile, resumes, applications, and interviews
- **Recruiters** can only manage their own company's jobs, applicants, and interviews
- **Admins** have read access to all platform data
- Role changes are restricted — users cannot self-assign admin roles

---

## Docker Setup

### Quick Start with Docker Compose

```bash
docker compose up --build
```

This starts:
- **PostgreSQL** at `localhost:5432` (data persisted in a Docker volume)
- **Spring Boot backend** at `localhost:8080`

### Building the Backend Image

```bash
cd backend
docker build -t hirehub-backend .
```

The multi-stage Dockerfile uses `eclipse-temurin:21-jre-alpine` for a lightweight runtime. Runs as a non-root user.

---

## API Documentation

### Swagger UI

In development, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

In production, Swagger is disabled by default.

### Actuator

Health check: `GET /actuator/health`

```
{"status":"UP","components":{"db":{"status":"UP"}}}
```

---

## Authentication

### Email/Password

- Access token: short-lived (15 min), returned in response body
- Refresh token: long-lived (7 days), stored as HttpOnly cookie
- Refresh token rotation: old token is revoked when a new one is issued
- Refresh token reuse detection: if a revoked token is reused, all tokens for that user are revoked

### Google/GitHub OAuth

1. Frontend obtains OAuth authorization code
2. Sends code to `POST /api/v1/auth/oauth/google` or `/auth/oauth/github`
3. Backend exchanges code, finds/creates user
4. Returns access token in body + refresh token in HttpOnly cookie

### Rate Limiting

Protected endpoints have configurable rate limits:
- Authentication: 10 requests/minute per IP
- AI features: 5 requests/minute per user
- Returns HTTP 429 when exceeded

---

## Running Tests

### Frontend

```bash
bun tsc -b --noEmit    # typecheck
```

### Backend

```bash
cd backend
mvn test               # run all tests (166+ tests)
```

---

## License

This project is private and not licensed for public use.
