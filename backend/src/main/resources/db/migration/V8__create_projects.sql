-- V8: student projects and their progress history.
-- A student profile has many projects. projects.current_stage / progress_percent mirror the latest
-- project_progress entry so lists do not need a join. ON DELETE RESTRICT: nothing is removed by cascade.

CREATE TABLE projects (
    id                 UUID                     PRIMARY KEY,
    student_profile_id UUID                     NOT NULL,
    title              VARCHAR(300)             NOT NULL,
    description        VARCHAR(4000),
    research_area      VARCHAR(150),
    supervisor_name    VARCHAR(150),
    deadline           DATE,
    status             VARCHAR(20)              NOT NULL DEFAULT 'PLANNING',
    current_stage      VARCHAR(30),
    progress_percent   INTEGER                  NOT NULL DEFAULT 0,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_student_profile FOREIGN KEY (student_profile_id)
        REFERENCES student_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT ck_projects_status CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED')),
    CONSTRAINT ck_projects_stage CHECK (current_stage IS NULL OR current_stage IN
        ('TOPIC_SELECTION', 'PROPOSAL', 'LITERATURE_REVIEW', 'METHODOLOGY', 'IMPLEMENTATION',
         'WRITING', 'REVIEW', 'DEFENSE_PREPARATION', 'SUBMISSION')),
    CONSTRAINT ck_projects_progress CHECK (progress_percent BETWEEN 0 AND 100)
);

CREATE INDEX idx_projects_student_profile ON projects (student_profile_id, created_at);

CREATE TABLE project_progress (
    id               UUID                     PRIMARY KEY,
    project_id       UUID                     NOT NULL,
    stage            VARCHAR(30)              NOT NULL,
    percent_complete INTEGER                  NOT NULL,
    note             VARCHAR(2000),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_progress_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_project_progress_stage CHECK (stage IN
        ('TOPIC_SELECTION', 'PROPOSAL', 'LITERATURE_REVIEW', 'METHODOLOGY', 'IMPLEMENTATION',
         'WRITING', 'REVIEW', 'DEFENSE_PREPARATION', 'SUBMISSION')),
    CONSTRAINT ck_project_progress_percent CHECK (percent_complete BETWEEN 0 AND 100)
);

CREATE INDEX idx_project_progress_project ON project_progress (project_id, created_at);
