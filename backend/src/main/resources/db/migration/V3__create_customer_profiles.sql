-- V3: customer profiles (User 1 ---- 0..1 CustomerProfile).
-- ON DELETE RESTRICT: a user that still has a profile cannot be deleted by accident.

CREATE TABLE customer_profiles (
    id                UUID                     PRIMARY KEY,
    user_id           UUID                     NOT NULL,
    customer_type     VARCHAR(20)              NOT NULL,
    organization_name VARCHAR(200),
    phone             VARCHAR(30),
    address           VARCHAR(300),
    city              VARCHAR(100),
    country           VARCHAR(100),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_customer_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_customer_profiles_type CHECK (customer_type IN
        ('INDIVIDUAL', 'BUSINESS', 'ORGANIZATION', 'INSTITUTION', 'STARTUP', 'OTHER'))
);
