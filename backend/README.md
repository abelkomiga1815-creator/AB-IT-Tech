# AB IT TECH SOLUTIONS — Main Backend (GROUP 3)

Spring Boot + PostgreSQL backend of **AB IT TECH SOLUTIONS — Technology Solutions, Digital Services & Student Platform**.

| Group | Responsibility | Technology |
|-------|----------------|------------|
| 1 | Corporate website (all customers) | Next.js, TypeScript |
| 2 | Student Project Platform | Next.js, TypeScript |
| **3** | **Main backend and database (this project)** | **Java 21, Spring Boot 3.5, PostgreSQL** |
| 4 | AI research and innovation engine | Python, FastAPI |

Current state: foundation, authentication, profiles, customer services (catalogue, inquiries, service requests), **student modules** (projects, progress, documents, defense preparation) and **research integration** (calls the AI engine, keeps the reports and a reading list). Part of the full system, see the repository README.

## What is included

- **Foundation:** `/api/v1/health`, Actuator, Flyway, global error format (`ApiResponse` / `ApiErrorResponse`), request-ID logging, dev/prod profiles.
- **Authentication:** register, login, `GET /auth/me`, BCrypt passwords, stateless JWT, roles `STUDENT` / `CUSTOMER` / `ADMIN`.
- **Profiles:** basic profile (`/profile`), customer profile (`/customer/profile`), student profile (`/student/profile`).
- **Customer services:** public service catalogue (the 7 agreed services), guest and customer inquiries, customer service requests with a status lifecycle, and minimal admin endpoints to answer them.
- **Student modules:** projects with progress history, document upload/download (local disk), and defense questions, all private to the owning student.
- **Research integration:** `ai/AiClient` calls the Python AI engine; `research/` stores analyses and saved papers per project.
- **Abuse protection:** per-client rate limits on login, registration and guest inquiries.
- **Tests:** unit, API and database-constraint tests that run on in-memory H2 (no PostgreSQL needed to run `mvn test`).

## Profile architecture

```text
                 USER (account: id, fullName, email, password, role, enabled)
                  |
        +---------+---------+
        |                   |
        v                   v
 CUSTOMER PROFILE     STUDENT PROFILE
 (0..1 per user)      (0..1 per user)
        |                   |
        v                   v
 Customer Inquiry      Student Projects -> Research / AI
 Service Requests      Documents, Defense Preparation
   (future)                 (future)
```

- The `users` table only holds what authentication needs. Customer and student data live in separate tables.
- A user has **at most one** profile of each type (database `UNIQUE (user_id)` constraint). Profiles are **not** created automatically at registration.
- Profile endpoints never take a user id: the owner is always the authenticated user.

## Roles and access

| Endpoint group | Public | STUDENT | CUSTOMER | ADMIN |
|----------------|:------:|:-------:|:--------:|:-----:|
| `GET /api/v1/health`, `POST /auth/register`, `POST /auth/login` | yes | yes | yes | yes |
| `GET /auth/me`, `GET/PUT /profile` | no | yes | yes | yes |
| `/api/v1/student/profile` (GET, POST, PUT) | no | **yes** | 403 | 403 |
| `/api/v1/customer/profile` (GET, POST, PUT) | no | 403 | **yes** | 403 |
| `/api/v1/customer/inquiries`, `/customer/service-requests` | no | 403 | **yes** | 403 |
| `/api/v1/student/projects/**` (progress, documents, defense questions) | no | **yes** | 403 | 403 |
| `/api/v1/admin/**` | no | 403 | 403 | **yes** |
| `GET /api/v1/services`, `POST /api/v1/inquiries` | yes | yes | yes | yes |

Registration accepts `STUDENT` or `CUSTOMER` (default `CUSTOMER`). `ADMIN` can never be chosen at registration; the first administrator is created directly in the database (see `docs/database/SCHEMA.md`). Admin endpoints live under `/api/v1/admin/**` (minimal, Stage 3).

## API endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/api/v1/health` | none | liveness check |
| POST | `/api/v1/auth/register` | none | create account |
| POST | `/api/v1/auth/login` | none | get JWT |
| GET | `/api/v1/auth/me` | any | current account |
| GET | `/api/v1/profile` | any | basic account info |
| PUT | `/api/v1/profile` | any | change `fullName` only |
| GET / POST / PUT | `/api/v1/customer/profile` | CUSTOMER | own customer profile |
| GET / POST / PUT | `/api/v1/student/profile` | STUDENT | own student profile |
| POST | `/api/v1/student/projects/{projectId}/research/analyses` | STUDENT | run a research analysis (calls the AI engine; at most one per project per minute) |
| GET | `/api/v1/student/projects/{projectId}/research/analyses[/{analysisId}]` | STUDENT | saved analyses |
| GET / POST / DELETE | `/api/v1/student/projects/{projectId}/research/sources[/{sourceId}]` | STUDENT | reading list |
| GET | `/api/v1/services`, `/api/v1/services/{slug}` | none | service catalogue |
| POST | `/api/v1/inquiries` | none | guest inquiry (website forms) |
| GET / POST | `/api/v1/customer/inquiries`, GET `/{id}` | CUSTOMER (profile required) | own inquiries |
| GET / POST | `/api/v1/customer/service-requests`, GET `/{id}`, POST `/{id}/cancel` | CUSTOMER (profile required) | own service requests |
| GET, PUT | `/api/v1/admin/inquiries`, `/{id}`, `/{id}/status` | ADMIN | staff: read and update inquiries |
| GET, PUT | `/api/v1/admin/service-requests`, `/{id}`, `/{id}/status` | ADMIN | staff: manage the request lifecycle |
| POST, GET, PUT | `/api/v1/student/projects`, `/{projectId}` | STUDENT (profile required) | own projects |
| POST, GET | `/api/v1/student/projects/{projectId}/progress` | STUDENT | progress history |
| POST (multipart), GET, DELETE | `/api/v1/student/projects/{projectId}/documents`, `/{documentId}/download`, `/{documentId}` | STUDENT | project documents |
| POST, GET, PUT, DELETE | `/api/v1/student/projects/{projectId}/defense-questions`, `/{questionId}` | STUDENT | defense preparation |

Full request/response examples and every error code: [`docs/api/API-REFERENCE.md`](docs/api/API-REFERENCE.md).

## Database tables

| Migration | Table | Notes |
|-----------|-------|-------|
| `V1__initial_database.sql` | `app_metadata` | foundation baseline |
| `V2__create_users.sql` | `users` | unique lower-case email, role check |
| `V3__create_customer_profiles.sql` | `customer_profiles` | `UNIQUE(user_id)`, FK `ON DELETE RESTRICT` |
| `V4__create_student_profiles.sql` | `student_profiles` | `UNIQUE(user_id)`, `UNIQUE(university, student_number)`, FK `ON DELETE RESTRICT` |
| `V5__create_services.sql` | `services` | seeded with the 7 agreed services; unique slug and name |
| `V6__create_customer_inquiries.sql` | `customer_inquiries` | nullable FK to `customer_profiles` (guests) and `services`; status check |
| `V7__create_service_requests.sql` | `service_requests` | FKs to `customer_profiles` and `services` (`RESTRICT`); status check |
| `V8__create_projects.sql` | `projects`, `project_progress` | FK to `student_profiles` (`RESTRICT`); status, stage and 0-100 percent checks |
| `V9__create_project_documents.sql` | `project_documents` | metadata only (file on disk); unique storage key; size > 0 |
| `V10__create_defense_questions.sql` | `defense_questions` | FK to `projects` (`RESTRICT`); category check |
| `V11__create_research.sql` | `research_analyses`, `research_sources` | report JSON per analysis; unique DOI per project; FKs `RESTRICT` |

Details and delete strategy: [`docs/database/SCHEMA.md`](docs/database/SCHEMA.md).

## Requirements

- **JDK 21** (`java -version`), **Maven 3.9+** (`mvn -version`), **PostgreSQL 14+**
- Optional: Docker, to start a local PostgreSQL with `docker compose up -d`

## Environment variables

| Variable | Required | Example |
|----------|----------|---------|
| `DATABASE_URL` | yes | `jdbc:postgresql://localhost:5432/abittech` |
| `DATABASE_USERNAME` | yes | `postgres` |
| `DATABASE_PASSWORD` | yes | `your_password_here` |
| `JWT_SECRET` | yes (min. 32 characters) | generate with `openssl rand -base64 48` |
| `JWT_EXPIRATION` | no (default `86400000` ms = 24 h) | `86400000` |
| `CORS_ALLOWED_ORIGINS` | no (default: none) | `http://localhost:3000,http://localhost:3001` |
| `AI_SERVICE_URL` | no (default `http://localhost:8000`) | `http://ai-service:8000` |
| `AI_SERVICE_API_KEY` | **yes** (min. 32 characters, same value as in the AI engine) | `openssl rand -base64 48` |
| `RATE_LIMIT_AUTH_PER_MINUTE`, `RATE_LIMIT_INQUIRY_PER_MINUTE` | no (10 and 5) | limits per client address |
| `FORWARD_HEADERS_STRATEGY` | no (`none`) | `framework`, only behind a trusted reverse proxy |
| `DOCUMENTS_DIR` | no (default `./data/documents`) | `/var/lib/abittech/documents` |
| `SERVER_PORT` | no (default 8080) | `8080` |
| `SPRING_PROFILES_ACTIVE` | no (default `dev`) | `prod` |

The values in `.env.example` are **examples only**. The application refuses to start if a required variable is missing or the JWT secret is too short.

```bash
cp .env.example .env      # then edit .env (it is git-ignored). Values without quotes.
```

`.env` is read automatically when you start the app from the `backend/` directory. Real environment variables override it.

## Run locally

```bash
cd backend
docker compose up -d      # optional: local PostgreSQL using the values from .env
mvn spring-boot:run
```

(Without Docker: create the database yourself, for example `CREATE DATABASE abittech;`, and point `DATABASE_URL` at it.)

## Run the tests

```bash
mvn clean test
```

Tests use H2 in PostgreSQL mode (profile `test`) and a built-in test JWT secret; no environment variables are required. Because H2 is not PostgreSQL, also start the application once against real PostgreSQL after each new migration: at startup Flyway applies the migrations and Hibernate validates the entities against the real schema.

## How to try the profile APIs (curl)

```bash
# 1. register a student and log in
curl -s -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"fullName":"John Doe","email":"john@example.com","password":"Str0ng-Passw0rd","role":"STUDENT"}'
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"john@example.com","password":"Str0ng-Passw0rd"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')

# 2. who am I / basic profile
curl -s localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"
curl -s -X PUT localhost:8080/api/v1/profile -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"fullName":"John A. Doe"}'

# 3. student profile
curl -s -X POST localhost:8080/api/v1/student/profile -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"university":"University of Dar es Salaam","programme":"Computer Science","yearOfStudy":3,"studentNumber":"2024/CS/001"}'
curl -s localhost:8080/api/v1/student/profile -H "Authorization: Bearer $TOKEN"
```

For a customer, register with `"role":"CUSTOMER"` and use `/api/v1/customer/profile` with `{"customerType":"BUSINESS","organizationName":"Acme Ltd"}`.

## Authentication requirements

Send `Authorization: Bearer <accessToken>` on every endpoint except the public ones. Missing/invalid/expired token → `401 UNAUTHORIZED`; valid token with the wrong role → `403 FORBIDDEN`. A disabled account is rejected immediately, even with a valid token.

## Future relationships

- **Customer Inquiry and Service Requests** (done in Stage 3): reference `customer_profiles.id`; a customer only sees their own. Details: [`docs/GROUP-3-CUSTOMER-SERVICES.md`](docs/GROUP-3-CUSTOMER-SERVICES.md).
- **Student Project Management** (done in Stage 4): projects reference `student_profiles.id`; progress, documents and defense questions hang off projects, and research analyses (Stage 5) will too. Details: [`docs/GROUP-3-STUDENT-MODULES.md`](docs/GROUP-3-STUDENT-MODULES.md).

## Folder structure

```text
backend/
├── pom.xml  README.md  .env.example  .gitignore  docker-compose.yml
├── docs/
│   ├── ARCHITECTURE.md
│   ├── GROUP-3-PROFILES.md
│   ├── GROUP-3-CUSTOMER-SERVICES.md
│   ├── GROUP-3-STUDENT-MODULES.md
│   ├── api/API-REFERENCE.md
│   ├── database/SCHEMA.md
│   └── troubleshooting/TROUBLESHOOTING.md
└── src/
    ├── main/java/com/abittech/platform/
    │   ├── Application.java
    │   ├── config/       properties, CORS, Clock
    │   ├── security/     SecurityConfig, JwtService, JWT filter, 401/403 JSON
    │   ├── common/       exception, response, logging, health, persistence, util, validation
    │   ├── user/         entity, repository, service, dto
    │   ├── auth/         controller, service, dto
    │   ├── profile/      controller, dto   (basic profile)
    │   ├── customer/     controller, service, repository, entity, dto
    │   ├── student/      controller, service, repository, entity, dto
    │   ├── service/      service catalogue (entity, repository, service, controller, dto)
    │   ├── inquiry/      guest, customer and admin inquiries
    │   ├── servicerequest/ customer service requests and admin lifecycle
    │   ├── project/      projects and progress history
    │   ├── document/     project documents (upload, storage, download)
    │   ├── defense/      defense preparation questions
    │   └── research/ ai/   (future, package-info only)
    ├── main/resources/   application*.yml, db/migration/V1..V11
    └── test/             tests mirror main; support/IntegrationTestBase; application-test.yml
```

## Security notes

- Passwords are stored as BCrypt hashes only; the hash is never returned. Secrets come from environment variables; `.env` is git-ignored.
- Ownership always comes from the JWT identity, never from a request id. Roles are checked in `SecurityConfig`.
- Error responses never contain stack traces, SQL, tokens or request values. Logs never contain passwords or tokens.
- JWTs are not revocable before they expire (no token blacklist yet); disabling the account stops them at once because the account is checked on every request.

## Limitations of this stage

Not implemented (later tasks): password reset, OTP, email verification, change password, social login, 2FA, an admin dashboard (only the minimal admin endpoints above exist), editing the service catalogue through the API (change it with a new migration), email notifications, rate limiting / spam protection for the public inquiry endpoint and for login, refresh tokens, research, AI, antivirus scanning and object storage for uploaded documents, deleting projects (they are archived instead).

## Research integration and rate limits (added with the full system)

- `ai/AiClient` calls the Python AI engine (`AI_SERVICE_URL`, header `X-API-Key`); `research/` stores each analysis (`research_analyses`) and the papers a student saves (`research_sources`). The slow AI call runs outside any database transaction. At most one analysis per project per minute.
- The engine being down gives `503 AI_SERVICE_UNAVAILABLE` and stores nothing; see `docs/api/API-REFERENCE.md`.
- Limits: not implemented are a shared (multi-instance) rate-limit store and virus scanning of uploads. Rate limits are per instance and in memory.
