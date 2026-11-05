-- V4: student profiles (User 1 ---- 0..1 StudentProfile).
-- ON DELETE RESTRICT: a user that still has a profile cannot be deleted by accident.
-- university/programme are plain text for now; they may become normalised tables later.

CREATE TABLE student_profiles (
    id             UUID                     PRIMARY KEY,
    user_id        UUID                     NOT NULL,
    university     VARCHAR(200)             NOT NULL,
    programme      VARCHAR(200)             NOT NULL,
    year_of_study  INTEGER,
    student_number VARCHAR(50),
    phone          VARCHAR(30),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_profiles_user UNIQUE (user_id),
    -- NULL student numbers are allowed and never conflict with each other.
    CONSTRAINT uq_student_profiles_university_number UNIQUE (university, student_number),
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_student_profiles_year CHECK (year_of_study IS NULL OR year_of_study BETWEEN 1 AND 10)
);
