-- V7: service requests (a customer commissions a service; this is the customer's "project").
-- Always belongs to a customer profile and a service. ON DELETE RESTRICT everywhere.

CREATE TABLE service_requests (
    id                  UUID                     PRIMARY KEY,
    customer_profile_id UUID                     NOT NULL,
    service_id          UUID                     NOT NULL,
    title               VARCHAR(200)             NOT NULL,
    description         VARCHAR(4000)            NOT NULL,
    status              VARCHAR(20)              NOT NULL DEFAULT 'SUBMITTED',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_requests_customer_profile FOREIGN KEY (customer_profile_id)
        REFERENCES customer_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_service_requests_service FOREIGN KEY (service_id)
        REFERENCES services (id) ON DELETE RESTRICT,
    CONSTRAINT ck_service_requests_status CHECK (status IN
        ('SUBMITTED', 'IN_REVIEW', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX idx_service_requests_customer_profile ON service_requests (customer_profile_id, created_at);
CREATE INDEX idx_service_requests_status_created ON service_requests (status, created_at);
