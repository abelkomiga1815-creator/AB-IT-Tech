-- V9: metadata of files uploaded to a project. The file itself lives in the configured documents
-- directory under storage_key (a random name); the original name is kept only for display/download.

CREATE TABLE project_documents (
    id                UUID                     PRIMARY KEY,
    project_id        UUID                     NOT NULL,
    document_type     VARCHAR(20)              NOT NULL,
    original_filename VARCHAR(255)             NOT NULL,
    content_type      VARCHAR(100)             NOT NULL,
    size_bytes        BIGINT                   NOT NULL,
    storage_key       VARCHAR(100)             NOT NULL,
    description       VARCHAR(500),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_documents_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_project_documents_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_project_documents_type CHECK (document_type IN
        ('PROPOSAL', 'THESIS_DRAFT', 'REFERENCE', 'DATA', 'PRESENTATION', 'OTHER')),
    CONSTRAINT ck_project_documents_size CHECK (size_bytes > 0)
);

CREATE INDEX idx_project_documents_project ON project_documents (project_id, created_at);
