# Troubleshooting

Start with the **request id**: every response has an `X-Request-Id` header and the same id appears in every server log line for that request (`INFO [<request-id>] ...`).

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| App stops at startup with `Could not resolve placeholder 'DATABASE_URL'` (or `_USERNAME`, `_PASSWORD`, `JWT_SECRET`) | required variable missing | create `.env` from `.env.example` in the `backend/` folder you start the app from, or export the variables |
| Startup error mentioning `app.jwt` / "JWT_SECRET must be at least 32 characters" | secret too short | use `openssl rand -base64 48` |
| `Connection to localhost:5432 refused` | PostgreSQL not running | `docker compose up -d` or start your local PostgreSQL |
| `password authentication failed` / `database "abittech" does not exist` | wrong credentials or database missing | check `DATABASE_*`; run `CREATE DATABASE abittech;` |
| Flyway: `Validate failed: Migration checksum mismatch` | an applied migration file was edited | restore the original file; put changes in a new `V<n+1>` migration |
| Flyway: `Detected failed migration` | a migration failed half-way | fix the cause, repair the history table (`flyway repair`) or recreate your **local** dev database |
| Hibernate: `Schema-validation: missing table / wrong column type` | entity and migration disagree | add a migration (never edit old ones) so the schema matches the entity |
| `401 UNAUTHORIZED` on a protected endpoint | no header, wrong format (`Bearer <token>`), expired token, token signed with another `JWT_SECRET`, or account disabled | log in again; check the header; check `JWT_SECRET` is the same in every running instance |
| `403 FORBIDDEN` | logged in with the wrong role (customer on `/student/**`, student on `/customer/**`, admin on both) | use an account with the right role |
| `401 INVALID_CREDENTIALS` on login | wrong email/password, unknown email, or disabled account (all intentionally look the same) | verify the credentials; check `users.enabled` |
| `404 CUSTOMER_PROFILE_NOT_FOUND` on `/customer/inquiries` or `/customer/service-requests` | customer has not created a profile yet | `POST /api/v1/customer/profile` first |
| `404 SERVICE_NOT_FOUND` when sending an inquiry or request | wrong or inactive `serviceSlug` | use a slug from `GET /api/v1/services` |
| `404 INQUIRY_NOT_FOUND` / `SERVICE_REQUEST_NOT_FOUND` | wrong id, or the item belongs to another customer (by design) | list your own items first |
| `409 INVALID_STATUS_TRANSITION` | status change not allowed | check the lifecycle in `docs/api/API-REFERENCE.md` |
| `403 FORBIDDEN` on `/api/v1/admin/**` | account is not an ADMIN | promote the account (see `docs/database/SCHEMA.md`), then log in again |
| `404 STUDENT_PROFILE_NOT_FOUND` on `/student/projects` | student has not created a profile yet | `POST /api/v1/student/profile` first |
| `404 PROJECT_NOT_FOUND` | wrong id, or the project belongs to another student (by design) | list your own projects |
| `415 UNSUPPORTED_FILE_TYPE` / `413 FILE_TOO_LARGE` on upload | extension not allowed / file over 10 MB | see the accepted types in the API reference |
| Startup error "Cannot create the documents directory" | `DOCUMENTS_DIR` not writable | point it at a writable folder |
| Uploaded documents disappeared after redeploying | `DOCUMENTS_DIR` was inside a container or build folder | mount a persistent volume and back it up |
| Startup error mentioning `app.ai` / "AI_SERVICE_API_KEY must be at least 32 characters" | AI key missing or short | set `AI_SERVICE_API_KEY` (same value as the AI engine) |
| `503 AI_SERVICE_UNAVAILABLE` on a research analysis | AI engine not running, wrong `AI_SERVICE_URL`, or timed out | check `docker compose ps` / `GET <AI>/health`; the backend log says "AI service not reachable" |
| `502 AI_SERVICE_ERROR` | the engine rejected our key (different `AI_SERVICE_API_KEY` on the two sides) or answered unexpectedly | compare the two keys; check the AI engine log by request id |
| `429 RATE_LIMITED` | too many login/registration/inquiry attempts from one address | wait a minute; behind a proxy set `FORWARD_HEADERS_STRATEGY=framework` so clients are told apart |
| `429 ANALYSIS_TOO_FREQUENT` | an analysis for this project ran less than a minute ago | wait |
| `409 EMAIL_ALREADY_EXISTS` | email already registered (case-insensitive) | log in instead |
| `409 *_PROFILE_ALREADY_EXISTS` | profile already created | use `PUT` to update |
| `404 *_PROFILE_NOT_FOUND` | profile not created yet | `POST` first |
| `400 VALIDATION_ERROR` | invalid field | read `errors[]` (field + message) |
| `400 INVALID_PROFILE_DATA` | organisation name missing for BUSINESS/ORGANIZATION/INSTITUTION/STARTUP | add `organizationName` |
| `400 BAD_REQUEST` "Malformed or unreadable request body" | invalid JSON or unknown enum value (for example `customerType`) | fix the JSON; see allowed values in the API reference |
| Browser shows a CORS error | origin not allowed | set `CORS_ALLOWED_ORIGINS=http://localhost:3000` (your frontend origin) and restart |
| `500 INTERNAL_ERROR` | unexpected server error | find the stack trace in the log using the `X-Request-Id` |
| `mvn test` fails on a test that cannot find a table/column | migration/entity mismatch | compare `db/migration` with the entity |
| `mvn test` behaves differently from PostgreSQL | tests use H2 | run the app once against PostgreSQL after every new migration |
| Port 8080 already in use | another process | set `SERVER_PORT=8081` |

## Useful checks

```bash
curl localhost:8080/api/v1/health          # API is up
curl localhost:8080/actuator/health        # includes the database check
java -version && mvn -version              # JDK 21, Maven 3.9+
```
