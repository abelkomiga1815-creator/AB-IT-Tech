-- V10: defense preparation. Students write the questions they expect and their prepared answers.

CREATE TABLE defense_questions (
    id         UUID                     PRIMARY KEY,
    project_id UUID                     NOT NULL,
    question   VARCHAR(1000)            NOT NULL,
    answer     VARCHAR(4000),
    category   VARCHAR(20)              NOT NULL DEFAULT 'GENERAL',
    prepared   BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_defense_questions_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_defense_questions_category CHECK (category IN
        ('GENERAL', 'METHODOLOGY', 'LITERATURE', 'RESULTS', 'IMPLEMENTATION', 'OTHER'))
);

CREATE INDEX idx_defense_questions_project ON defense_questions (project_id, created_at);
