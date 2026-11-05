# Database schema

PostgreSQL, managed only by Flyway (`src/main/resources/db/migration`). Hibernate runs with `ddl-auto: validate`.

```text
users (id UUID PK, full_name, email UNIQUE lower-case, password_hash, role, enabled, created_at, updated_at)
  |  1
  +--- 0..1 customer_profiles (id PK, user_id UNIQUE FK, customer_type, organization_name, phone, address, city, country, created_at, updated_at)
  |
  +--- 0..1 student_profiles  (id PK, user_id UNIQUE FK, university, programme, year_of_study, student_number, phone, created_at, updated_at)

app_metadata (meta_key PK, meta_value, created_at)   -- foundation baseline

services (id PK, slug UNIQUE, name UNIQUE, summary, description, display_order, active, timestamps)
  |  1                                   |  1
  +--- 0..n customer_inquiries           +--- 0..n service_requests
         (customer_profile_id NULL, service_id NULL, name, email, phone, message, status, timestamps)
                                               (customer_profile_id NOT NULL, service_id NOT NULL, title, description, status, timestamps)
customer_profiles 1 ---- 0..n customer_inquiries
customer_profiles 1 ---- 0..n service_requests
```

| Table | Constraints |
|-------|-------------|
| `users` | PK `id`; `uq_users_email`; `ck_users_email_lowercase` (email = LOWER(email)); `ck_users_role` (STUDENT, CUSTOMER, ADMIN) |
| `customer_profiles` | PK; `uq_customer_profiles_user`; FK `fk_customer_profiles_user` -> users `ON DELETE RESTRICT`; `ck_customer_profiles_type` |
| `student_profiles` | PK; `uq_student_profiles_user`; `uq_student_profiles_university_number` (NULL numbers allowed); FK -> users `ON DELETE RESTRICT`; `ck_student_profiles_year` (1–10 or NULL) |

| `services` | PK; `uq_services_slug`; `uq_services_name`; `ck_services_slug_lowercase`; seeded by V5 with fixed ids |
| `customer_inquiries` | PK; FK -> `customer_profiles` and `services` (`RESTRICT`, both nullable); `ck_inquiries_status` (NEW, IN_PROGRESS, RESOLVED); indexes on profile and (status, created_at) |
| `service_requests` | PK; FK -> `customer_profiles` and `services` (`RESTRICT`, NOT NULL); `ck_service_requests_status`; indexes on (profile, created_at) and (status, created_at) |

| `projects` | PK; FK -> `student_profiles` (`RESTRICT`); `ck_projects_status`, `ck_projects_stage`, `ck_projects_progress` (0-100); index (profile, created_at) |
| `project_progress` | PK; FK -> `projects` (`RESTRICT`); stage check; percent 0-100 |
| `project_documents` | PK; FK -> `projects` (`RESTRICT`); unique `storage_key`; type check; `size_bytes > 0` |
| `defense_questions` | PK; FK -> `projects` (`RESTRICT`); category check |

```text
student_profiles 1 ---- 0..n projects 1 ---- 0..n project_progress
                                      1 ---- 0..n project_documents   (file bytes on disk, see DOCUMENTS_DIR)
                                      1 ---- 0..n defense_questions
```

| `research_analyses` | PK; FK -> `projects` `RESTRICT`; similarity level check (low, moderate, high, insufficient_data); `report_json` TEXT |
| `research_sources` | PK; FK -> `projects` `RESTRICT`; `uq_research_sources_project_doi` (NULL DOIs allowed); year range check |

```text
projects 1 ---- 0..n research_analyses    (full AI report as JSON text, title snapshot)
         1 ---- 0..n research_sources     (papers saved to the reading list)
```

## Delete strategy

Foreign keys use `ON DELETE RESTRICT`: the database refuses to delete a user who still has a profile, a profile that still has projects, or a project that still has documents or questions, so nothing disappears through a cascade. Projects are archived (`status = ARCHIVED`), not deleted. Only a student's own documents and defense questions can be deleted, explicitly, through the API (the uploaded file is removed with its record). Use `users.enabled = false` to deactivate an account. If real deletion is ever required (for example a data-protection request), add a reviewed procedure that removes dependent rows explicitly and in order.

## Migration rules

- Files are named `V<n>__description.sql`, applied in order, and **never edited after being applied** (Flyway verifies checksums).
- Schema changes = a new migration. No `DROP DATABASE/SCHEMA/TABLE` or `TRUNCATE` (a test enforces this); `flyway clean` is disabled.
- Prefer backward-compatible steps: add nullable columns, backfill, then tighten.

## Creating the first administrator

There is no admin registration endpoint (by design). After registering a normal account, promote it directly in PostgreSQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

Use your own address; do this only on your own database.
