-- V11: research assistance results.
-- research_analyses keeps the report returned by the AI engine (as JSON text) so it can be shown again
-- without calling the external sources. research_sources holds papers a student chose to save.
-- ON DELETE RESTRICT everywhere: nothing disappears through a cascade.

CREATE TABLE research_analyses (
    id               UUID                     PRIMARY KEY,
    project_id       UUID                     NOT NULL,
    project_title    VARCHAR(300)             NOT NULL,
    similarity_level VARCHAR(20)              NOT NULL,
    papers_found     INTEGER                  NOT NULL,
    report_json      TEXT                     NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_research_analyses_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_research_analyses_level CHECK (similarity_level IN
        ('low', 'moderate', 'high', 'insufficient_data')),
    CONSTRAINT ck_research_analyses_papers CHECK (papers_found >= 0)
);

CREATE INDEX idx_research_analyses_project ON research_analyses (project_id, created_at);

CREATE TABLE research_sources (
    id               UUID                     PRIMARY KEY,
    project_id       UUID                     NOT NULL,
    title            VARCHAR(500)             NOT NULL,
    authors          VARCHAR(1000),
    publication_year INTEGER,
    venue            VARCHAR(300),
    doi              VARCHAR(200),
    url              VARCHAR(1000),
    provider         VARCHAR(50),
    note             VARCHAR(1000),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_research_sources_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE RESTRICT,
    -- a DOI can be saved once per project (NULL DOIs never conflict)
    CONSTRAINT uq_research_sources_project_doi UNIQUE (project_id, doi),
    CONSTRAINT ck_research_sources_year CHECK (publication_year IS NULL OR publication_year BETWEEN 1000 AND 2100)
);

CREATE INDEX idx_research_sources_project ON research_sources (project_id, created_at);
