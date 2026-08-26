-- V3: Add missing columns to resumes table for V3.5a resume management

ALTER TABLE resumes
    ADD COLUMN file_size BIGINT NOT NULL DEFAULT 0;

ALTER TABLE resumes
    ADD COLUMN content_type VARCHAR(100) NOT NULL DEFAULT 'application/pdf';

ALTER TABLE resumes
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Add a partial unique index so only one default resume per student
CREATE UNIQUE INDEX uq_resumes_student_default
    ON resumes (student_id)
    WHERE is_default = TRUE;
