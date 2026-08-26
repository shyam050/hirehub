-- HireHub V3.1 — Initial Schema
-- UUID primary keys, audit columns, proper relationships

-- ============================================================
-- ENUMS
-- ============================================================

CREATE TYPE user_role AS ENUM ('STUDENT', 'RECRUITER', 'ADMIN');

CREATE TYPE application_stage AS ENUM (
    'APPLIED', 'SCREENING', 'SHORTLISTED', 'TECHNICAL_INTERVIEW',
    'HR_INTERVIEW', 'OFFERED', 'SELECTED', 'REJECTED'
);

CREATE TYPE job_type AS ENUM ('FULL_TIME', 'PART_TIME', 'INTERNSHIP', 'CONTRACT');

CREATE TYPE job_status AS ENUM ('ACTIVE', 'CLOSED', 'DRAFT');

CREATE TYPE interview_type AS ENUM (
    'ONLINE_TEST', 'TECHNICAL', 'HR', 'MANAGERIAL'
);

CREATE TYPE interview_status AS ENUM (
    'SCHEDULED', 'COMPLETED', 'CANCELLED', 'RESCHEDULED'
);

CREATE TYPE notification_type AS ENUM ('APPLICATION', 'JOB', 'INTERVIEW', 'SYSTEM');

-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) UNIQUE,
    name            VARCHAR(255),
    image           TEXT,
    role            user_role,
    role_selected_at TIMESTAMPTZ,
    is_anonymous    BOOLEAN DEFAULT FALSE,
    email_verified  BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);

-- ============================================================
-- STUDENTS
-- ============================================================

CREATE TABLE students (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    phone            VARCHAR(30),
    university       VARCHAR(255),
    degree           VARCHAR(255),
    field_of_study   VARCHAR(255),
    graduation_year  INTEGER,
    gpa              VARCHAR(20),
    bio              TEXT,
    location         VARCHAR(255),
    linkedin         VARCHAR(500),
    github           VARCHAR(500),
    portfolio        VARCHAR(500),
    skills           TEXT[],           -- PostgreSQL array
    education        JSONB DEFAULT '[]'::jsonb,
    projects         JSONB DEFAULT '[]'::jsonb,
    profile_complete BOOLEAN DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_students_user_id ON students (user_id);

-- ============================================================
-- RECRUITERS
-- ============================================================

CREATE TABLE recruiters (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id  UUID,
    job_title   VARCHAR(255),
    phone       VARCHAR(30),
    bio         TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recruiters_user_id ON recruiters (user_id);

-- ============================================================
-- COMPANIES
-- ============================================================

CREATE TABLE companies (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    industry       VARCHAR(255),
    size           VARCHAR(100),
    website        VARCHAR(500),
    logo           VARCHAR(500),
    location       VARCHAR(255),
    founded_year   INTEGER,
    approved       BOOLEAN DEFAULT FALSE,
    created_by     UUID NOT NULL REFERENCES users(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_companies_approved ON companies (approved);
CREATE INDEX idx_companies_created_by ON companies (created_by);

-- Now add the FK on recruiters
ALTER TABLE recruiters
    ADD CONSTRAINT fk_recruiters_company
    FOREIGN KEY (company_id) REFERENCES companies(id);

-- ============================================================
-- JOBS
-- ============================================================

CREATE TABLE jobs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id            UUID NOT NULL REFERENCES companies(id),
    posted_by             UUID NOT NULL REFERENCES users(id),
    recruiter_id          UUID NOT NULL REFERENCES recruiters(id),
    title                 VARCHAR(500) NOT NULL,
    description           TEXT NOT NULL,
    requirements          TEXT[],
    skills                TEXT[],
    location              VARCHAR(255) NOT NULL,
    job_type              job_type NOT NULL DEFAULT 'FULL_TIME',
    remote                BOOLEAN DEFAULT FALSE,
    salary_min            NUMERIC,
    salary_max            NUMERIC,
    experience_min        NUMERIC,
    experience_max        NUMERIC,
    education_required    VARCHAR(255),
    application_deadline  TIMESTAMPTZ NOT NULL,
    status                job_status NOT NULL DEFAULT 'ACTIVE',
    application_count     INTEGER DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_company_id ON jobs (company_id);
CREATE INDEX idx_jobs_status ON jobs (status);
CREATE INDEX idx_jobs_posted_by ON jobs (posted_by);

-- ============================================================
-- APPLICATIONS
-- ============================================================

CREATE TABLE applications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id       UUID NOT NULL REFERENCES jobs(id),
    student_id   UUID NOT NULL REFERENCES students(id),
    recruiter_id UUID NOT NULL REFERENCES recruiters(id),
    company_id   UUID NOT NULL REFERENCES companies(id),
    status       application_stage NOT NULL DEFAULT 'APPLIED',
    cover_letter TEXT,
    timeline     JSONB DEFAULT '[]'::jsonb,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_application_per_job UNIQUE (job_id, student_id)
);

CREATE INDEX idx_applications_job_id ON applications (job_id);
CREATE INDEX idx_applications_student_id ON applications (student_id);
CREATE INDEX idx_applications_company_id ON applications (company_id);
CREATE INDEX idx_applications_recruiter_id ON applications (recruiter_id);

-- ============================================================
-- RESUMES
-- ============================================================

CREATE TABLE resumes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID NOT NULL REFERENCES students(id),
    file_name       VARCHAR(500) NOT NULL,
    storage_id      VARCHAR(500) NOT NULL,
    extracted_text  TEXT,
    is_default      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resumes_student_id ON resumes (student_id);

-- ============================================================
-- INTERVIEWS
-- ============================================================

CREATE TABLE interviews (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id   UUID NOT NULL REFERENCES applications(id),
    job_id           UUID NOT NULL REFERENCES jobs(id),
    student_id       UUID NOT NULL REFERENCES students(id),
    company_id       UUID NOT NULL REFERENCES companies(id),
    recruiter_id     UUID NOT NULL REFERENCES recruiters(id),
    interview_type   interview_type NOT NULL,
    scheduled_at     TIMESTAMPTZ NOT NULL,
    duration         INTEGER NOT NULL,            -- minutes
    meeting_link     VARCHAR(1000),
    interviewer_name VARCHAR(255),
    status           interview_status NOT NULL DEFAULT 'SCHEDULED',
    notes            TEXT,
    feedback         TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interviews_application_id ON interviews (application_id);
CREATE INDEX idx_interviews_student_id ON interviews (student_id);
CREATE INDEX idx_interviews_company_id ON interviews (company_id);
CREATE INDEX idx_interviews_job_id ON interviews (job_id);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================

CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id),
    title      VARCHAR(500) NOT NULL,
    message    TEXT NOT NULL,
    type       notification_type NOT NULL,
    link       VARCHAR(1000),
    read       BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, "read");
