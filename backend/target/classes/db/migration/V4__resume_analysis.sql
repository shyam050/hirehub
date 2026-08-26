-- V4: Create resume_analyses table for AI-powered resume analysis

CREATE TABLE resume_analyses (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id              UUID NOT NULL REFERENCES students(id),
    resume_id               UUID NOT NULL REFERENCES resumes(id),
    overall_score           INTEGER NOT NULL DEFAULT 0,
    extracted_skills        JSONB DEFAULT '[]'::jsonb,
    extracted_education     JSONB DEFAULT '[]'::jsonb,
    extracted_projects      JSONB DEFAULT '[]'::jsonb,
    extracted_experience    JSONB DEFAULT '[]'::jsonb,
    extracted_certifications JSONB DEFAULT '[]'::jsonb,
    extracted_achievements  JSONB DEFAULT '[]'::jsonb,
    strengths               JSONB DEFAULT '[]'::jsonb,
    weaknesses              JSONB DEFAULT '[]'::jsonb,
    missing_skills          JSONB DEFAULT '[]'::jsonb,
    recommendations         JSONB DEFAULT '[]'::jsonb,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resume_analyses_student_id ON resume_analyses (student_id);
CREATE INDEX idx_resume_analyses_resume_id ON resume_analyses (resume_id);
CREATE INDEX idx_resume_analyses_resume_created ON resume_analyses (resume_id, created_at DESC);
