# HireHub Backend (V3.7)

Spring Boot 3.x + PostgreSQL backend with JWT authentication for the HireHub recruitment platform.

## Tech Stack

- **Java 21**
- **Spring Boot 3.4.x**
- **Spring Data JPA** (Hibernate)
- **Spring Security** (JWT Bearer, stateless)
- **PostgreSQL** (production/dev) / **H2** (tests)
- **Flyway** (database migrations)
- **JJWT 0.12.x** (JWT creation/validation)
- **BCrypt** (password hashing)
- **Apache PDFBox** (PDF text extraction)
- **Spring WebFlux** (OpenAI HTTP client)
- **Lombok**
- **JUnit 5 + Mockito**

## API Endpoints

### Public

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/health` | Health check |
| `POST` | `/api/v1/auth/register` | Register student or recruiter |
| `POST` | `/api/v1/auth/login` | Login with email + password |
| `POST` | `/api/v1/auth/refresh` | Exchange refresh token |
| `POST` | `/api/v1/auth/oauth/google` | Google OAuth sign-in/sign-up |
| `POST` | `/api/v1/auth/oauth/github` | GitHub OAuth sign-in/sign-up |
| `POST` | `/api/v1/auth/oauth/role` | Set role for OAuth user |
| `GET` | `/api/v1/jobs` | Browse/search active jobs |
| `GET` | `/api/v1/jobs/{id}` | View job details |
| `GET` | `/api/v1/companies/{id}` | View company profile |

### Protected — Auth

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/logout` | Any | Revoke refresh tokens |
| `GET` | `/api/v1/auth/me` | Any | Current user profile |

### OAuth Flow

1. Frontend obtains OAuth authorization code from Google/GitHub
2. Frontend sends code to `POST /api/v1/auth/oauth/google` or `POST /api/v1/auth/oauth/github`
3. Backend exchanges code for tokens, validates identity, finds/creates user
4. If existing user → returns JWT tokens + user
5. If new user (role=null) → returns `requiresRole: true` + temporary token
6. Frontend calls `POST /api/v1/auth/oauth/role` with `{ role: "STUDENT" | "RECRUITER", temporaryToken }`
7. Backend creates user profile + returns JWT tokens

### Protected — Users

| Method | Path | Role | Description |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | Any | Own user profile |
| `PATCH` | `/api/v1/users/me` | Any | Update name/image |
| `GET` | `/api/v1/users/{id}` | ADMIN | Any user profile |

### Protected — Students

| Method | Path | Role | Description |
|---|---|---|---|
| `GET` | `/api/v1/students/me` | STUDENT | Own student profile |
| `PUT` | `/api/v1/students/me` | STUDENT | Update student profile |
| `GET` | `/api/v1/students/{id}` | STUDENT/ADMIN | Student profile |

### Protected — Recruiters

| Method | Path | Role | Description |
|---|---|---|---|
| `GET` | `/api/v1/recruiters/me` | RECRUITER | Own recruiter profile |
| `PUT` | `/api/v1/recruiters/me` | RECRUITER | Update recruiter profile |

### Protected — Companies

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/companies` | RECRUITER | Create company |
| `PUT` | `/api/v1/companies/{id}` | RECRUITER (owner) / ADMIN | Update company |
| `POST` | `/api/v1/companies/{id}/approve` | ADMIN | Approve company |
| `POST` | `/api/v1/companies/{id}/reject` | ADMIN | Reject company |

### Protected — Jobs

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/jobs` | RECRUITER | Create job posting |
| `PUT` | `/api/v1/jobs/{id}` | RECRUITER (owner) / ADMIN | Update job |
| `DELETE` | `/api/v1/jobs/{id}` | RECRUITER (owner) / ADMIN | Delete job |
| `POST` | `/api/v1/jobs/{id}/close` | RECRUITER (owner) / ADMIN | Close job |

### Protected — Applications

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/jobs/{jobId}/applications` | STUDENT | Apply for job |
| `GET` | `/api/v1/applications/me` | STUDENT | Own applications |
| `GET` | `/api/v1/applications/{id}` | Any (owner) | Application details |
| `GET` | `/api/v1/jobs/{jobId}/applications` | RECRUITER (owner) / ADMIN | Job applicants |
| `PATCH` | `/api/v1/applications/{id}/status` | RECRUITER (owner) / ADMIN | Update status |

### Protected — Resumes

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/resumes` | STUDENT | Upload PDF resume |
| `GET` | `/api/v1/resumes` | STUDENT | List own resumes |
| `GET` | `/api/v1/resumes/{id}` | STUDENT/ADMIN | Resume metadata |
| `GET` | `/api/v1/resumes/{id}/download` | STUDENT/ADMIN | Download PDF |
| `DELETE` | `/api/v1/resumes/{id}` | STUDENT | Delete resume |
| `POST` | `/api/v1/resumes/{id}/default` | STUDENT | Set default resume |

### Protected — Interviews

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/interviews` | RECRUITER | Schedule interview |
| `GET` | `/api/v1/interviews/me` | STUDENT | Own interviews |
| `GET` | `/api/v1/interviews/{id}` | Any (owner) | Interview details |
| `GET` | `/api/v1/interviews/application/{id}` | Any (owner) | App interviews |
| `GET` | `/api/v1/interviews/recruiter` | RECRUITER/ADMIN | Company interviews |
| `PATCH` | `/api/v1/interviews/{id}/reschedule` | RECRUITER (owner) | Reschedule |
| `POST` | `/api/v1/interviews/{id}/cancel` | RECRUITER (owner) | Cancel |
| `POST` | `/api/v1/interviews/{id}/complete` | RECRUITER (owner) | Complete |
| `POST` | `/api/v1/interviews/{id}/feedback` | RECRUITER (owner) | Submit feedback |

### Protected — Notifications

| Method | Path | Role | Description |
|---|---|---|---|
| `GET` | `/api/v1/notifications` | Any | List notifications |
| `GET` | `/api/v1/notifications/unread-count` | Any | Unread count |
| `PATCH` | `/api/v1/notifications/{id}/read` | Any | Mark as read |
| `PATCH` | `/api/v1/notifications/read-all` | Any | Mark all read |

### Protected — AI Resume Analysis (V3.6a)

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/resumes/{id}/analyze` | STUDENT | Analyze with AI |
| `GET` | `/api/v1/resumes/{id}/analyses` | STUDENT/ADMIN | List analyses |
| `GET` | `/api/v1/resumes/{id}/analyses/latest` | STUDENT/ADMIN | Latest analysis |
| `GET` | `/api/v1/resume-analyses/me` | STUDENT | All my analyses |

### Protected — AI Job Matching (V3.6b)

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/jobs/{jobId}/match` | STUDENT | Calculate match |
| `GET` | `/api/v1/jobs/{jobId}/match` | STUDENT | Get match for job |
| `GET` | `/api/v1/job-matches/me` | STUDENT | All my matches |
| `GET` | `/api/v1/job-matches/me/skill-gaps` | STUDENT | Aggregated skill gaps |
| `GET` | `/api/v1/resumes/{resumeId}/matches` | STUDENT/ADMIN | Matches for resume |

### Protected — AI Mock Interview Coach (V3.6c)

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/ai-interviews` | STUDENT | Start interview |
| `GET` | `/api/v1/ai-interviews/me` | STUDENT | List my interviews |
| `GET` | `/api/v1/ai-interviews/{id}` | STUDENT (owner) / ADMIN | Interview details |
| `GET` | `/api/v1/ai-interviews/{id}/questions` | STUDENT (owner) | Interview questions |
| `GET` | `/api/v1/ai-interviews/{id}/report` | STUDENT (owner) | Final report |
| `POST` | `/api/v1/ai-interviews/{id}/next-question` | STUDENT (owner) | Generate next question |
| `POST` | `/api/v1/ai-interviews/{id}/questions/{qNum}/answer` | STUDENT (owner) | Submit answer + eval |
| `POST` | `/api/v1/ai-interviews/{id}/complete` | STUDENT (owner) | Complete + get report |
| `POST` | `/api/v1/ai-interviews/{id}/abandon` | STUDENT (owner) | Abandon interview |

## AI Architecture (V3.6a + V3.6b + V3.6c)

```
Controllers
    ↓
Services (ResumeAnalysisService / JobMatchingService / AiInterviewService)
    ↓
AiService (interface)
    ↓
OpenAiService (implementation)
```

Clean dependency injection — business services never depend on OpenAI directly.

### AI Resume Analysis Flow

```
Student clicks Analyze →
  1. Verify ownership
  2. Check 7-day cache
  3. Extract text from PDF (Apache PDFBox)
  4. Send to OpenAI
  5. Validate structured response
  6. Save analysis
  7. Return result
```

### AI Job Matching Flow

```
Student clicks Match →
  1. Verify student identity + resume ownership
  2. Check 7-day cache (student + job + resume)
  3. Deterministic skill overlap calculation (30%)
  4. AI profile fit analysis (70%)
  5. Blend scores
  6. Save match
  7. Return result
```

### AI Mock Interview Flow

```
Student starts interview →
  1. Verify student owns resume
  2. Generate Q1 via AI
  3. Student answers → AI evaluates
  4. AI generates adaptive next question
  5. Repeat until all questions answered
  6. AI generates final report
  7. Scores grounded in question evaluations
```

### Matching Algorithm

```
Final Score = AI Score × 0.70 + Deterministic Score × 0.30
```

**Deterministic component** (SkillMatchingService):
- Normalizes skills (lowercase, common variations)
- Compares required skills against student skills
- Calculates overlap percentage
- Identifies matched and missing skills

**AI component** (OpenAiService):
- Evaluates overall profile fit
- Identifies strengths and recommendations
- Provides explanation

### Interview Question Adaptation

- Strong previous answer → harder next question
- Weak previous answer → foundational follow-up
- Questions adapted to job requirements + resume
- Never repeats previous questions
- Category-aware (TECHNICAL, BEHAVIORAL, HR, PROJECT, RESUME)

### Final Report Scoring

- Technical score → average of technical/project question scores
- Communication score → average of behavioral/HR question scores
- Problem-solving score → overall average
- Qualitative feedback via AI (grounded in actual evaluations)

### Cost Controls

- **7-day analysis/match cache** prevents repeated AI calls
- **GET endpoints never trigger AI** — explicitly tested
- **Text truncated** before sending to AI
- **60-second timeout** on OpenAI requests
- **User must explicitly request** analysis/matching/interview
- **One AI call per question** — no bulk generation
- **Idempotent question creation** — no duplicates on page refresh

## Skill Gap Aggregation

`GET /api/v1/job-matches/me/skill-gaps` returns:

```json
[
  {"skill": "Kubernetes", "count": 5},
  {"skill": "Kafka", "count": 3},
  {"skill": "Redis", "count": 2}
]
```

Deterministic aggregation — no AI involved. Sorted by frequency descending.

## Database Schema

| Table | Purpose |
|---|---|
| `users` | Authentication, roles |
| `students` | Student profiles |
| `recruiters` | Recruiter profiles |
| `companies` | Company info, approval status |
| `jobs` | Job postings |
| `applications` | Job applications with timeline |
| `resumes` | PDF resume storage + metadata |
| `interviews` | Interview scheduling |
| `notifications` | In-app alerts |
| `refresh_tokens` | Server-side refresh token storage |
| `resume_analyses` | AI resume analysis results |
| `job_matches` | AI job matching results |
| `ai_interviews` | AI mock interview sessions |
| `ai_interview_questions` | AI mock interview questions + evaluations |
| `oauth_providers` | OAuth provider links (Google, GitHub) |

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | Yes | Dev-only | Base64-encoded signing key |
| `DB_URL` | No | localhost:5432/hirehub_dev | PostgreSQL URL |
| `DB_USERNAME` | No | hirehub | Database username |
| `DB_PASSWORD` | No | hirehub_dev | Database password |
| `OPENAI_API_KEY` | Yes (for AI) | — | OpenAI API key |
| `OPENAI_MODEL` | No | gpt-4o | AI model |
| `OPENAI_BASE_URL` | No | https://api.openai.com | API base URL |
| `AI_CACHE_DAYS` | No | 7 | Cache duration |
| `GOOGLE_CLIENT_ID` | No | — | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | No | — | Google OAuth2 client secret |
| `GITHUB_CLIENT_ID` | No | — | GitHub OAuth client ID |
| `GITHUB_CLIENT_SECRET` | No | — | GitHub OAuth client secret |

## Project Structure

```
src/main/java/com/hirehub/
├── HireHubApplication.java
├── config/  (CORS, Security, JWT entry point)
├── auth/  (Auth controller, service, JWT, filters, OAuth)
├── common/  (enums, exceptions, ApiResponse)
├── health/  (HealthController)
├── user/  (UserController, UserService)
├── student/  (StudentController, StudentService)
├── recruiter/  (RecruiterController, RecruiterService)
├── company/  (CompanyController, CompanyService)
├── job/  (JobController, JobService)
├── application/  (ApplicationController, ApplicationService)
├── resume/  (ResumeController, ResumeService, storage/)
├── interview/  (InterviewController, InterviewService)
├── notification/  (NotificationController, NotificationService)
├── ai/  (AiService, OpenAiService, SkillMatchingService, config/)
├── resumeanalysis/  (ResumeAnalysisController, Service, entity, repository)
├── jobmatching/  (JobMatchingController, Service, entity, repository)
├── aiinterview/  (AiInterviewController, Service, entities, repositories)
└── resume/text/  (PDF text extraction)

src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_auth_fields.sql
├── V3__resume_schema_updates.sql
├── V4__resume_analysis.sql
├── V5__job_matching.sql
├── V6__ai_interviews.sql
└── V7__oauth_providers.sql

src/test/java/com/hirehub/
├── HireHubApplicationTests.java
├── HealthEndpointTests.java
├── ai/SkillMatchingServiceTest.java  (9 tests)
├── auth/AuthIntegrationTests.java  (18 tests)
├── auth/OAuthIntegrationTests.java  (14 tests)
├── api/ApiIntegrationTests.java  (21 tests)
├── api/JobApplicationTests.java  (20 tests)
├── api/ResumeTests.java  (21 tests)
├── api/InterviewTests.java  (22 tests)
├── api/ResumeAnalysisTests.java  (13 tests)
├── api/JobMatchingTests.java  (10 tests)
├── api/AiInterviewTests.java  (11 tests)
└── common/exception/GlobalExceptionHandlerTests.java  (7 tests)
```

## Tests

```bash
cd backend
mvn test
```

152 tests total covering:
- Application context loading
- Health endpoint
- Global exception handling (7)
- Registration & login (18)
- User, Student, Recruiter, Company APIs (21)
- Job creation, search, authorization (20)
- Resume management (21)
- Interview scheduling, lifecycle (22)
- AI Resume Analysis (13)
- AI Job Matching (10)
- AI Mock Interview Coach (11)
- SkillMatchingService unit tests (9)

## Roadmap

| Phase | Status |
|---|---|
| V3.1 — Foundation | ✅ |
| V3.2 — JWT Authentication | ✅ |
| V3.3 — User, Student, Recruiter, Company APIs | ✅ |
| V3.4 — Jobs and Applications APIs | ✅ |
| V3.5a — Resume Management + PDF Storage | ✅ |
| V3.5b — Interview Management + Notifications | ✅ |
| V3.6a — AI Resume Analysis | ✅ |
| V3.6b — AI Job Matching | ✅ |
| V3.6c — AI Mock Interview Coach | ✅ |
| V3.7 — OAuth + Frontend Migration | ✅ |
| V3.8 — Redis Caching | Planned |
| V3.8 — Docker + Deployment | Planned |
| V3.9 — Frontend Integration | Planned |
| V3.10 — Full Migration from Convex | Planned |
