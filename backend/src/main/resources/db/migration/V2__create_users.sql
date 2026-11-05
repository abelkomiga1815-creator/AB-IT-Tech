-- V2: authentication accounts.
-- Holds only what is needed to authenticate; customer/student details live in profile tables.

CREATE TABLE users (
    id            UUID                     PRIMARY KEY,
    full_name     VARCHAR(150)             NOT NULL,
    email         VARCHAR(255)             NOT NULL,
    password_hash VARCHAR(100)             NOT NULL,
    role          VARCHAR(20)              NOT NULL,
    enabled       BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = LOWER(email)),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'CUSTOMER', 'ADMIN'))
);
