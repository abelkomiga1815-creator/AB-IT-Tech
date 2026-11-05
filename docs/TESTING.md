# Testing and compiling: what is verified, what is not, and how to do the first full run

## 1. Honest status

All code was written in an environment that had **no Maven, no npm registry access, no Docker and no internet**. So:

| Part | Verified in the authoring environment | NOT verified |
|------|----------------------------------------|--------------|
| `scripts/verify-consistency.py` | **run: 76 of 76 checks pass** (service catalogue, enums, error codes, AI contract, env variables, migrations, secrets, forbidden patterns) | |
| AI engine core (`ai-service`) | **run: 91 unit tests pass** (keywords, similarity, gaps, titles, providers' request building and parsing, search service, config, security, pipeline, report) | the FastAPI layer and its 14 tests (`tests/test_api.py`), the real httpx client, any call to the real sources |
| Student platform / website | syntax check of all TS/TSX files with TypeScript; the pure-logic modules were executed (proxy allow-list, formatting, upload checks, safe links, inquiry payloads) | `npm install`, type check, lint, Vitest, `next build`, running pages |
| Backend | Java syntax check of all files; every project import resolves; no field injection; YAML/XML/SQL well-formed | **compilation against Spring, all tests, running against PostgreSQL**, Flyway on a real database |
| Docker / Compose / smoke test | shell syntax of the smoke test; Compose file reviewed by eye | `docker compose build/up`, the proxy add-on, the smoke test itself |

Treat the first full run as the real test. Expect some compile errors and a few failing tests; the list in section 4 shows where I think they are most likely.

## 2. First full run, in this order

```bash
# 0. static cross-checks (no setup needed)
python3 scripts/verify-consistency.py

# 1. AI engine (Python 3.11+)
cd ai-service
python -m venv .venv && source .venv/bin/activate
pip install -r requirements-dev.txt
pytest                                    # expect 105 passed (91 core + 14 HTTP-layer)
cd ..

# 2. Backend (JDK 21, Maven 3.9+). No PostgreSQL needed for the tests (they use H2).
cd backend
mvn clean test                            # expect all test classes green (36 test classes)
cd ..

# 3. Frontends (Node 20+), once each
cd corporate-website && npm install && npm run typecheck && npm run lint && npm test && npm run build && cd ..
cd student-platform  && npm install && npm run typecheck && npm run lint && npm test && npm run build && cd ..

# 4. The whole system
cp .env.example .env                      # set the secrets
docker compose up -d --build
docker compose ps                         # wait until postgres, ai-service and backend are healthy
scripts/smoke-test.sh                     # expect 0 failed (the research step may SKIP without internet)
```

Also start the backend once against real PostgreSQL (step 4 does this): the tests use H2, and only a real start validates the entities against the PostgreSQL schema (`ddl-auto: validate`).

## 3. When something fails

Send the **first** error only (later ones are often consequences), with the file name and line, and say which command produced it:

| Where | Typical message | Likely cause |
|-------|-----------------|--------------|
| Maven compile | `cannot find symbol`, `incompatible types` | a method signature I assumed from memory; fix is usually one line |
| Maven tests | `Schema-validation: wrong column type` | an entity and a migration disagree (check enum or TEXT columns) |
| Maven tests | `NoSuchBeanDefinition`, `UnsatisfiedDependency` | wiring: a missing `@EnableConfigurationProperties` entry or bean |
| Maven tests | `Could not resolve placeholder` | a property missing in `application-test.yml` |
| Maven tests | `Flyway ... H2` syntax error | a migration statement H2 (PostgreSQL mode) rejects |
| npm typecheck | `Type ... is not assignable` | a typing detail in a component or a Next.js route handler signature |
| npm lint | `react-hooks/exhaustive-deps`, `@next/next/...` | a rule I did not anticipate; usually a one-line change |
| npm build | `Module not found` | an import path; paths use the `@/` alias for `src/` |
| pytest | `ModuleNotFoundError: fastapi` | `pip install -r requirements-dev.txt` was not run in the active virtualenv |
| docker compose | `variable is not set` | a required value missing in `.env` |
| docker compose | backend restarts | read `docker compose logs backend`: usually a missing secret (under 32 characters) or an unreachable database |
| smoke test | a FAIL line | the line names the check; run the same curl by hand and send the answer |

## 4. Where I am least sure (check these first)

Backend:
- `@MockitoBean` (Spring 6.2 / Boot 3.4+) in `ResearchApiTest`; `MockRestServiceServer.bindTo(RestClient.Builder)` in `AiClientTest`.
- `@Column(columnDefinition = "TEXT")` on `ResearchAnalysis.reportJson` against `ddl-auto: validate` on PostgreSQL.
- The test profile uses `ddl-auto: none` on purpose (H2 versus enum column types); the real start validates.
- `HandlerMethodValidationException` and `MaxUploadSizeExceededException` handlers (Spring 6.2 class names).
- jjwt 0.12 builder/parser calls in `JwtService`; Flyway with H2 in tests (`flyway-core` only).
- `TransactionTemplate` bean injection in `ResearchService`.

Frontends:
- Next.js 15 route handler context type (`params: Promise<...>`) and `cookies()` being async.
- `eslint` 8 with `eslint-config-next` 15 and the rules `react-hooks/exhaustive-deps` / `@typescript-eslint/*` on `useLoad` and the panels.
- Strict TypeScript on generic helpers in `lib/api.ts`.
- `output: "standalone"` in the Docker images (no lock file exists, so `npm install` picks current versions).

AI engine:
- FastAPI/Starlette `TestClient` with `raise_server_exceptions=False`, and the exception handler for plain `Exception`.
- Response formats of the three external APIs (parsers follow their documented shapes; only fixtures were used). Do one live call per source and compare.

## 5. Quality gates worth adding next

CI that runs sections 0 to 3 on every push; `npm audit`, `pip-audit` and OWASP dependency-check; a nightly restore test of the backup; a Playwright (or similar) browser test of register, create project, upload, research, download.
