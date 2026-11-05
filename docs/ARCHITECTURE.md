# System architecture

## Components

| Component | Responsibility | Talks to |
|-----------|----------------|----------|
| Corporate website | Public pages; sends visitor inquiries from the browser | backend (public endpoints `GET /services`, `POST /inquiries`) |
| Student platform | Student UI. Its own server side (route handlers) keeps the login token in an httpOnly cookie and forwards API calls | backend |
| Backend (Spring Boot) | The only component with the database and the document files; authentication, authorization, all business rules | PostgreSQL, AI engine |
| AI engine (FastAPI) | Stateless analysis of a project against published papers | OpenAlex, Crossref, Semantic Scholar |
| PostgreSQL | System of record, schema owned by Flyway migrations V1 to V11 | |

Rule: **browsers never reach the database or the AI engine**, and the AI engine never reaches the database. All data access goes through the backend.

## Main flows

### Student signs in and works on a project
1. The browser posts credentials to the student platform (`/api/auth/login`). Its server calls `POST /api/v1/auth/login` on the backend and, if the account is a STUDENT account, stores the returned JWT in an httpOnly, SameSite=Strict cookie. The token never reaches browser JavaScript.
2. Page code calls `/api/backend/...` on the same origin. The route handler checks the path against an allow-list, adds `Authorization: Bearer <token>` from the cookie and forwards the call.
3. The backend authenticates the JWT, reloads the account from the database (so disabling an account takes effect at once), checks the role, and derives the owner of the data from the token. Projects, documents and defense questions are reachable only through the owner's student profile.

### Research analysis
1. Student clicks "Run research analysis" on their project.
2. The backend checks ownership and the one-per-minute limit, then (outside any database transaction) calls the AI engine `POST /api/v1/research/report` with the project title and description and the shared `X-API-Key`.
3. The AI engine extracts key terms, queries the three sources with **only those terms**, merges and ranks the papers, computes the similarity indicator, gap suggestions with evidence, related terms and title ideas, and returns one report.
4. The backend stores the report (`research_analyses`) and returns it. The student can save papers to a reading list (`research_sources`).
5. If the engine is down the student gets a clear 503 and nothing is stored; if one of the three sources fails, the report says which one.

### Visitor sends an inquiry
The website posts `{name, email, phone?, serviceSlug?, message}` straight to `POST /api/v1/inquiries` (CORS allows the website origin only, requests are rate limited). Staff read inquiries through the minimal `/api/v1/admin/inquiries` endpoints.

## Data ownership and relationships

```text
users 1-- 0..1 customer_profiles 1-- 0..n customer_inquiries (guests: no profile)
                                 1-- 0..n service_requests --> services (the 7 agreed services)
users 1-- 0..1 student_profiles  1-- 0..n projects 1-- 0..n project_progress
                                                   1-- 0..n project_documents (files on disk)
                                                   1-- 0..n defense_questions
                                                   1-- 0..n research_analyses (AI report JSON)
                                                   1-- 0..n research_sources (saved papers)
```

All foreign keys are `ON DELETE RESTRICT`: nothing is removed by a cascade. Accounts are disabled, projects are archived.

## Key decisions

- **One API, one database owner.** Keeps authorization in one place and lets later clients (mobile apps) reuse the same API.
- **Backend-for-frontend in the student platform.** The JWT stays out of browser JavaScript (protects against token theft by XSS) and no CORS is needed for that app. The corporate website only calls public endpoints and holds no secrets.
- **Ownership comes from the token, never from request ids.** No endpoint takes a user id for ordinary operations; other users' items answer 404.
- **Honest AI output.** Retrieved facts and the tool's interpretation are separate; every gap suggestion carries evidence; similarity is an indicator with a fixed disclaimer; source failures are reported as such, never as "nothing exists".
- **Deterministic analysis, no language model.** Explainable and testable; the AI engine's `gap_analysis` and `recommendations` packages are the place to add a model later.
- **Migrations are append-only.** Versioned Flyway files, never edited after use, no destructive SQL (enforced by a test), `flyway clean` disabled.
- **Fail fast on configuration.** The backend refuses to start without database settings, a 32+ character JWT secret and a 32+ character AI key; the AI engine refuses to start without its key.

## Ports and networks

Compose publishes 3000, 3001 and 8080 on 127.0.0.1 only; PostgreSQL and the AI engine have no published port. For a public deployment only an HTTPS reverse proxy is exposed (`deploy/docker-compose.proxy.yml`).
