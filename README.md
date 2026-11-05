# AB IT TECH SOLUTIONS: platform

Technology solutions, digital services and a student project platform, in one repository.

| Folder | What it is | Technology | Port |
|--------|------------|------------|------|
| `corporate-website/` | Public company website for all customers (services, contact, request a service) | Next.js, TypeScript | 3000 |
| `student-platform/` | Platform for university and college students (projects, progress, documents, defense preparation, research assistance) | Next.js, TypeScript | 3001 |
| `backend/` | The one REST API and owner of the database (accounts, profiles, catalogue, inquiries, projects, research results) | Java 21, Spring Boot 3, PostgreSQL, Flyway | 8080 |
| `ai-service/` | AI research engine: related papers, similarity indicator, gap suggestions | Python, FastAPI | 8000 (internal) |
| `docs/` | System documentation | Markdown | |
| `scripts/` | `verify-consistency.py` (static cross-checks), `smoke-test.sh` (end-to-end check of a running system) | | |

```text
 browsers
   |  corporate website (3000)           student platform (3001)
   |        |  public API calls               |  same-origin calls, session in an httpOnly cookie
   |        v                                 v
   +----> Spring Boot API (8080) <------------+        owns the database and the uploaded files
                |             \
                v              v
           PostgreSQL     Python AI engine (8000, internal, shared API key)
                                  |
                      OpenAlex, Crossref, Semantic Scholar
```

## Quick start (everything in Docker)

```bash
cp .env.example .env          # edit it: set the passwords and the two secrets (see comments inside)
docker compose up -d --build
scripts/smoke-test.sh         # optional: checks the running system end to end
```

Then open http://localhost:3000 (company website) and http://localhost:3001 (student platform). The API is on http://localhost:8080.
The first administrator is promoted in the database: see `docs/DEPLOYMENT.md`.

## Working on one part

Each folder has its own README with run, test and configuration instructions:
[`backend`](backend/README.md) · [`ai-service`](ai-service/README.md) · [`corporate-website`](corporate-website/README.md) · [`student-platform`](student-platform/README.md).

## Documentation

| File | Content |
|------|---------|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | components, data flows, decisions |
| [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) | local and production deployment, HTTPS, backups, upgrades, first admin |
| [`docs/SECURITY-REVIEW.md`](docs/SECURITY-REVIEW.md) | what is protected, how it was checked, known gaps before production |
| [`docs/TESTING.md`](docs/TESTING.md) | **how to compile and test everything, what is verified and what is not** |

## Status

All features of the plan are implemented. **Nothing has been compiled or run end to end yet**: the code was written in an environment without Maven, npm, Docker or network access. What was verified there is listed in [`docs/TESTING.md`](docs/TESTING.md) (static checks, the AI engine's core logic tests, the frontends' pure logic). The next step is the compile-and-test pass described in that file.

## What is deliberately not included

Password reset, change password, e-mail verification, social login and two-factor authentication; an admin dashboard (only minimal admin endpoints exist); refresh tokens and token revocation; notifications by e-mail; a language model in the AI engine (it uses transparent text statistics); virus scanning of uploads.
