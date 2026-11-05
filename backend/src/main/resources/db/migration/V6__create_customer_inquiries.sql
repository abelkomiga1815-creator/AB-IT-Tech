-- V6: customer inquiries.
-- Guests can submit inquiries (customer_profile_id is NULL); logged-in customers link theirs to
-- their customer profile. Foreign keys use ON DELETE RESTRICT so no inquiry is lost by accident.

CREATE TABLE customer_inquiries (
    id                  UUID                     PRIMARY KEY,
    customer_profile_id UUID,
    service_id          UUID,
    name                VARCHAR(150)             NOT NULL,
    email               VARCHAR(255)             NOT NULL,
    phone               VARCHAR(30),
    message             VARCHAR(4000)            NOT NULL,
    status              VARCHAR(20)              NOT NULL DEFAULT 'NEW',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inquiries_customer_profile FOREIGN KEY (customer_profile_id)
        REFERENCES customer_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inquiries_service FOREIGN KEY (service_id)
        REFERENCES services (id) ON DELETE RESTRICT,
    CONSTRAINT ck_inquiries_status CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED'))
);

CREATE INDEX idx_inquiries_customer_profile ON customer_inquiries (customer_profile_id);
CREATE INDEX idx_inquiries_status_created ON customer_inquiries (status, created_at);
