-- V5: service catalogue, seeded with the seven agreed services.
-- Services are never deleted by the application; hide one by setting active = FALSE.

CREATE TABLE services (
    id            UUID                     PRIMARY KEY,
    slug          VARCHAR(100)             NOT NULL,
    name          VARCHAR(150)             NOT NULL,
    summary       VARCHAR(300)             NOT NULL,
    description   VARCHAR(2000)            NOT NULL,
    display_order INTEGER                  NOT NULL DEFAULT 0,
    active        BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_services_slug UNIQUE (slug),
    CONSTRAINT uq_services_name UNIQUE (name),
    CONSTRAINT ck_services_slug_lowercase CHECK (slug = LOWER(slug))
);

INSERT INTO services (id, slug, name, summary, description, display_order) VALUES
    ('3f6c1a10-0001-4b1e-9a00-000000000001', 'software-development', 'Software Development', 'Custom software built around how your organization actually works.', 'We design, build and maintain business software, from internal tools and automation to full systems that integrate with the tools you already use.', 1),
    ('3f6c1a10-0002-4b1e-9a00-000000000002', 'web-development', 'Web Development', 'Fast, secure and responsive websites and web applications.', 'From a professional company website to an interactive web platform, we build for speed, accessibility and growth.', 2),
    ('3f6c1a10-0003-4b1e-9a00-000000000003', 'mobile-app-development', 'Mobile App Development', 'Mobile apps that people enjoy using.', 'We turn ideas into reliable mobile experiences that connect to your systems and serve your users on the devices they carry.', 3),
    ('3f6c1a10-0004-4b1e-9a00-000000000004', 'cybersecurity', 'Cybersecurity', 'Practical protection for your systems, data and people.', 'We help you understand your risks and put sensible safeguards in place before problems happen.', 4),
    ('3f6c1a10-0005-4b1e-9a00-000000000005', 'data-analysis', 'Data Analysis', 'Turn raw data into decisions you can trust.', 'We clean, analyse and present your data so that managers and teams can see what is happening and act on it.', 5),
    ('3f6c1a10-0006-4b1e-9a00-000000000006', 'ui-ux-graphic-design', 'UI/UX & Graphic Design', 'Clear, attractive design for products and brands.', 'We design interfaces people understand at first glance and visual materials that present your organization well.', 6),
    ('3f6c1a10-0007-4b1e-9a00-000000000007', 'it-support-maintenance', 'IT Support & Maintenance', 'Dependable support so your technology keeps working.', 'We keep your computers, networks and software running smoothly and help your team when something goes wrong.', 7);
