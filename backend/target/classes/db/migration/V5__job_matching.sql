-- V5: Create job_matches table for AI job matching

CREATE TABLE job_matches (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id          UUID NOT NULL REFERENCES students(id),
    job_id              UUID NOT NULL REFERENCES jobs(id),
    resume_id           UUID NOT NULL REFERENCES resumes(id),
    match_score         INTEGER NOT NULL DEFAULT 0,
    matched_skills      JSONB DEFAULT '[]'::jsonb,
    missing_skills      JSONB DEFAULT '[]'::jsonb,
    strengths           JSONB DEFAULT '[]'::jsonb,
    recommendations     JSONB DEFAULT '[]'::jsonb,
    explanation         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_matches_student_id ON job_matches (student_id);
CREATE INDEX idx_job_matches_job_id ON job_matches (job_id);
CREATE INDEX idx_job_matches_resume_id ON job_matches (resume_id);
CREATE UNIQUE INDEX uq_job_matches_student_job_resume ON job_matches (student_id, job_id, resume_id);
