-- V1: foundation baseline.
--
-- Creates the single table needed by the GROUP 3 foundation: a small key/value table that
-- records which foundation stage the schema is at. It gives Flyway a first versioned
-- migration and lets connectivity and migration state be verified.
--
-- Business tables (users, customers, services, projects, documents, ...) are added by later
-- versioned migrations (V2__..., V3__...). Never edit this file after it has been applied.

CREATE TABLE app_metadata (
    meta_key   VARCHAR(100)             PRIMARY KEY,
    meta_value VARCHAR(500)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_metadata (meta_key, meta_value)
VALUES ('foundation.stage', 'GROUP-3-BACKEND-DATABASE-FOUNDATION');
