-- V6: Create AI mock interview tables

CREATE TYPE ai_interview_type AS ENUM ('TECHNICAL', 'HR', 'BEHAVIORAL', 'MIXED');
CREATE TYPE ai_interview_difficulty AS ENUM ('EASY', 'MEDIUM', 'HARD');
CREATE TYPE ai_interview_status AS ENUM ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED');
CREATE TYPE question_category AS ENUM ('TECHNICAL', 'BEHAVIORAL', 'HR', 'PROJECT', 'RESUME');

CREATE TABLE ai_interviews (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id              UUID NOT NULL REFERENCES students(id),
    job_id                  UUID NOT NULL REFERENCES jobs(id),
    resume_id               UUID NOT NULL REFERENCES resumes(id),
    interview_type          ai_interview_type NOT NULL,
    difficulty              ai_interview_difficulty NOT NULL DEFAULT 'MEDIUM',
    total_questions         INTEGER NOT NULL DEFAULT 5,
    current_question_number INTEGER NOT NULL DEFAULT 0,
    status                  ai_interview_status NOT NULL DEFAULT 'NOT_STARTED',
    overall_score           INTEGER,
    technical_score         INTEGER,
    communication_score     INTEGER,
    problem_solving_score   INTEGER,
    strengths               JSONB DEFAULT '[]'::jsonb,
    weaknesses              JSONB DEFAULT '[]'::jsonb,
    missing_concepts        JSONB DEFAULT '[]'::jsonb,
    recommended_topics      JSONB DEFAULT '[]'::jsonb,
    overall_feedback        TEXT,
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_interviews_student_id ON ai_interviews (student_id);
CREATE INDEX idx_ai_interviews_job_id ON ai_interviews (job_id);
CREATE INDEX idx_ai_interviews_student_job ON ai_interviews (student_id, job_id);

CREATE TABLE ai_interview_questions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id        UUID NOT NULL REFERENCES ai_interviews(id),
    question_number     INTEGER NOT NULL,
    question            TEXT NOT NULL,
    category            question_category NOT NULL,
    expected_topics     JSONB DEFAULT '[]'::jsonb,
    student_answer      TEXT,
    score               INTEGER,
    strengths           JSONB DEFAULT '[]'::jsonb,
    weaknesses          JSONB DEFAULT '[]'::jsonb,
    feedback            TEXT,
    missing_concepts    JSONB DEFAULT '[]'::jsonb,
    ideal_answer_points JSONB DEFAULT '[]'::jsonb,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_ai_question_per_interview UNIQUE (interview_id, question_number)
);

CREATE INDEX idx_ai_questions_interview_id ON ai_interview_questions (interview_id);
CREATE INDEX idx_ai_questions_interview_number ON ai_interview_questions (interview_id, question_number);
