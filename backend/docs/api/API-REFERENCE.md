# API reference

Base path: `/api/v1`. All bodies are JSON. Protected endpoints need `Authorization: Bearer <accessToken>`.

Success: `{"success": true, "data": {...}}`
Error: `{"success": false, "code": "...", "message": "...", "timestamp": "2026-01-01T10:00:00Z"}` (validation errors add `"errors": [{"field": "...", "message": "..."}]`)

Every response carries an `X-Request-Id` header; send one yourself (8–64 chars `A-Za-z0-9._-`) to correlate logs.

## Health

`GET /api/v1/health` (public) -> `200 {"success": true, "status": "UP"}`
`GET /actuator/health` (public) -> `{"status": "UP"}`, includes the database check.

## Authentication

### POST /api/v1/auth/register (public)
```json
{"fullName": "John Doe", "email": "john@example.com", "password": "Str0ng-Passw0rd", "role": "STUDENT"}
```
`role` optional: `STUDENT` or `CUSTOMER` (default `CUSTOMER`). Password 8–72 characters. Email is trimmed and lower-cased.
`201` -> `{"success": true, "data": {"id": "...", "fullName": "John Doe", "email": "john@example.com", "role": "STUDENT", "enabled": true}}`

### POST /api/v1/auth/login (public)
```json
{"email": "john@example.com", "password": "Str0ng-Passw0rd"}
```
`200` -> `{"success": true, "data": {"accessToken": "<jwt>", "tokenType": "Bearer", "expiresIn": 86400, "user": {...}}}`

### GET /api/v1/auth/me
`200` -> same user object as above (never a password).

## Basic profile

`GET /api/v1/profile` -> user object.
`PUT /api/v1/profile` body `{"fullName": "Jane Smith"}` -> updated user object. Only `fullName` is applied; other properties are ignored.

## Customer profile (role CUSTOMER)

`GET /api/v1/customer/profile` -> 200 profile | 404 `CUSTOMER_PROFILE_NOT_FOUND`
`POST /api/v1/customer/profile` -> 201 | 409 `CUSTOMER_PROFILE_ALREADY_EXISTS`
`PUT /api/v1/customer/profile` -> 200 | 404 `CUSTOMER_PROFILE_NOT_FOUND`

Body (POST and PUT):
```json
{"customerType": "BUSINESS", "organizationName": "Acme Ltd", "phone": "+255 712 345 678",
 "address": "12 Main Street", "city": "Dar es Salaam", "country": "Tanzania"}
```
Response data: `id, customerType, organizationName, phone, address, city, country, createdAt, updatedAt`.

## Student profile (role STUDENT)

`GET /api/v1/student/profile` -> 200 | 404 `STUDENT_PROFILE_NOT_FOUND`
`POST /api/v1/student/profile` -> 201 | 409 `STUDENT_PROFILE_ALREADY_EXISTS` or `STUDENT_NUMBER_ALREADY_EXISTS`
`PUT /api/v1/student/profile` -> 200 | 404 | 409 `STUDENT_NUMBER_ALREADY_EXISTS`

Body (POST and PUT):
```json
{"university": "University of Dar es Salaam", "programme": "Computer Science",
 "yearOfStudy": 3, "studentNumber": "2024/CS/001", "phone": "+255 755 000 111"}
```
Response data: `id, university, programme, yearOfStudy, studentNumber, phone, createdAt, updatedAt`.

## Service catalogue (public)

`GET /api/v1/services` -> `data`: list of `{slug, name, summary, description}` (active services, in display order).
`GET /api/v1/services/{slug}` -> one service | 404 `SERVICE_NOT_FOUND`.

## Inquiries

`POST /api/v1/inquiries` (public, used by the website forms)
```json
{"name": "Jane Doe", "email": "jane@example.com", "phone": "+255 712 345 678", "serviceSlug": "web-development", "message": "We need a website for our company."}
```
`phone` and `serviceSlug` are optional; `message` needs 10-4000 characters. `201` -> `{"data": {"id": "...", "status": "NEW", "createdAt": "..."}}` (no personal data is echoed back). Unknown service -> 404 `SERVICE_NOT_FOUND`.

Customer (role CUSTOMER, customer profile required, otherwise 404 `CUSTOMER_PROFILE_NOT_FOUND`):
- `POST /api/v1/customer/inquiries` body `{"serviceSlug": "cybersecurity", "message": "..."}` -> 201. Name, email and phone come from the account/profile.
- `GET /api/v1/customer/inquiries` -> own inquiries, newest first.
- `GET /api/v1/customer/inquiries/{id}` -> own inquiry | 404 `INQUIRY_NOT_FOUND` (also for other customers' ids).

Admin (role ADMIN):
- `GET /api/v1/admin/inquiries?status=NEW&page=0&size=20` -> `{"data": {"items": [...], "page", "size", "totalItems", "totalPages"}}` (size max 100).
- `GET /api/v1/admin/inquiries/{id}`, `PUT /api/v1/admin/inquiries/{id}/status` body `{"status": "IN_PROGRESS"}` (NEW, IN_PROGRESS, RESOLVED).

## Service requests

Customer (role CUSTOMER, customer profile required):
- `POST /api/v1/customer/service-requests` body `{"serviceSlug": "web-development", "title": "Company website", "description": "At least 10 characters ..."}` -> 201, status `SUBMITTED`.
- `GET /api/v1/customer/service-requests`, `GET /api/v1/customer/service-requests/{id}` (404 `SERVICE_REQUEST_NOT_FOUND` for other customers' ids).
- `POST /api/v1/customer/service-requests/{id}/cancel` -> allowed while `SUBMITTED` or `IN_REVIEW`, otherwise 409 `INVALID_STATUS_TRANSITION`.

Admin (role ADMIN):
- `GET /api/v1/admin/service-requests?status=&page=&size=`, `GET /api/v1/admin/service-requests/{id}` (includes customer name, email, organisation).
- `PUT /api/v1/admin/service-requests/{id}/status` body `{"status": "IN_REVIEW"}`.

Lifecycle: `SUBMITTED -> IN_REVIEW -> ACCEPTED -> IN_PROGRESS -> COMPLETED`; `CANCELLED` from any open status; `REJECTED` from `SUBMITTED`/`IN_REVIEW`. `COMPLETED`, `REJECTED`, `CANCELLED` are final. Anything else -> 409 `INVALID_STATUS_TRANSITION`.

## Student projects (role STUDENT, student profile required)

Without a student profile every endpoint answers 404 `STUDENT_PROFILE_NOT_FOUND`. A project that does not exist **or belongs to another student** answers 404 `PROJECT_NOT_FOUND`.

- `POST /api/v1/student/projects` body `{"title": "...", "description": "...", "researchArea": "...", "supervisorName": "...", "deadline": "2027-06-30"}` (only `title` required) -> 201, status `PLANNING`.
- `GET /api/v1/student/projects` -> own projects, newest first. `GET /api/v1/student/projects/{projectId}`.
- `PUT /api/v1/student/projects/{projectId}` same fields plus required `status` (`PLANNING`, `IN_PROGRESS`, `COMPLETED`, `ARCHIVED`). There is no delete; archive instead.

Project data: `id, title, description, researchArea, supervisorName, deadline, status, currentStage, progressPercent, createdAt, updatedAt`.

### Progress
- `POST /api/v1/student/projects/{projectId}/progress` body `{"stage": "WRITING", "percentComplete": 60, "note": "optional"}` -> 201. Stages: `TOPIC_SELECTION, PROPOSAL, LITERATURE_REVIEW, METHODOLOGY, IMPLEMENTATION, WRITING, REVIEW, DEFENSE_PREPARATION, SUBMISSION`. `percentComplete` 0-100. The project's `currentStage` and `progressPercent` follow the latest entry.
- `GET /api/v1/student/projects/{projectId}/progress` -> history, newest first.

### Documents
- `POST /api/v1/student/projects/{projectId}/documents` as `multipart/form-data`: part `file` (required), field `type` (`PROPOSAL, THESIS_DRAFT, REFERENCE, DATA, PRESENTATION, OTHER`, default `OTHER`), field `description` (optional, max 500) -> 201 `{id, type, fileName, contentType, sizeBytes, description, createdAt}`.
  Accepted extensions: pdf, doc, docx, ppt, pptx, xls, xlsx, csv, txt, md, png, jpg, jpeg. Max size 10 MB. The content type is derived from the extension, not from the client.
- `GET /api/v1/student/projects/{projectId}/documents` -> list, newest first.
- `GET /api/v1/student/projects/{projectId}/documents/{documentId}/download` -> the file as an attachment (`Content-Disposition`, `X-Content-Type-Options: nosniff`).
- `DELETE /api/v1/student/projects/{projectId}/documents/{documentId}` -> removes the record and the file.

### Defense questions
- `POST /api/v1/student/projects/{projectId}/defense-questions` body `{"question": "...", "answer": "optional", "category": "GENERAL", "prepared": false}` -> 201. Categories: `GENERAL, METHODOLOGY, LITERATURE, RESULTS, IMPLEMENTATION, OTHER`.
- `GET` (oldest first), `PUT .../{questionId}` (replace), `DELETE .../{questionId}`.

## Research assistance (role STUDENT, own projects only)

- `POST /api/v1/student/projects/{id}/research/analyses` -> 201 with `{id, projectTitle, similarityLevel, papersFound, createdAt, report}`. Sends the project title and description to the AI engine, which can take up to a minute. At most one analysis per project per minute (429 `ANALYSIS_TOO_FREQUENT`). Errors: 503 `AI_SERVICE_UNAVAILABLE` (engine unreachable or timed out; nothing is stored), 502 `AI_SERVICE_ERROR` (unexpected answer or wrong shared key), 400 `BAD_REQUEST` if the title has fewer than 3 characters.
- `GET .../research/analyses` -> summaries (`id, projectTitle, similarityLevel, papersFound, createdAt`), newest first. `GET .../research/analyses/{analysisId}` -> the full report (404 `RESEARCH_ANALYSIS_NOT_FOUND`).
- `POST .../research/sources` body `{"title", "authors", "year", "venue", "doi", "url", "provider", "note"}` (only `title` required; `url` must be http(s)) -> 201; 409 `RESEARCH_SOURCE_ALREADY_EXISTS` for the same DOI (or title without DOI).
- `GET .../research/sources`, `DELETE .../research/sources/{sourceId}`.

The `report` object is exactly what the AI engine returns: `retrieved` (queries, per-source status, papers) and `analysis` (keywords, similarity indicator with disclaimer, gap suggestions with evidence, related terms, title ideas), plus `methodology` and `notices`. See `ai-service/docs/AI-SERVICE.md`.

## Rate limits (public endpoints)

`POST /auth/login` and `/auth/register` share a limit (default 10 per minute per client address); `POST /inquiries` has its own (default 5). Exceeding it returns 429 `RATE_LIMITED` with a `Retry-After` header.

## Error codes

| HTTP | code | Meaning |
|------|------|---------|
| 400 | `VALIDATION_ERROR` | field validation failed (see `errors[]`) |
| 400 | `BAD_REQUEST` | malformed JSON, wrong parameter type, unknown enum value |
| 400 | `INVALID_ROLE` | registration with `ADMIN` |
| 400 | `INVALID_PROFILE_DATA` | profile business rule failed (organisation name required for this customer type) |
| 401 | `UNAUTHORIZED` | missing, invalid, expired token, or disabled account |
| 401 | `INVALID_CREDENTIALS` | wrong email or password (same answer for unknown email and disabled account) |
| 403 | `FORBIDDEN` | authenticated but wrong role |
| 404 | `RESOURCE_NOT_FOUND` | unknown URL or entity |
| 404 | `CUSTOMER_PROFILE_NOT_FOUND`, `STUDENT_PROFILE_NOT_FOUND` | profile not created yet |
| 400 | `INVALID_FILE` | upload missing or empty |
| 404 | `PROJECT_NOT_FOUND` | no such project, or it belongs to another student |
| 404 | `DOCUMENT_NOT_FOUND`, `DEFENSE_QUESTION_NOT_FOUND` | no such item in this student's project |
| 413 | `FILE_TOO_LARGE` | upload larger than 10 MB |
| 415 | `UNSUPPORTED_FILE_TYPE` | extension not on the whitelist |
| 404 | `SERVICE_NOT_FOUND` | unknown or inactive service slug |
| 404 | `INQUIRY_NOT_FOUND`, `SERVICE_REQUEST_NOT_FOUND` | no such item, or it belongs to someone else |
| 429 | `RATE_LIMITED`, `ANALYSIS_TOO_FREQUENT` | too many requests; wait and retry |
| 502 / 503 | `AI_SERVICE_ERROR`, `AI_SERVICE_UNAVAILABLE` | the research engine misbehaved / could not be reached |
| 404 | `RESEARCH_ANALYSIS_NOT_FOUND`, `RESEARCH_SOURCE_NOT_FOUND` | no such item, or it belongs to another student |
| 409 | `RESEARCH_SOURCE_ALREADY_EXISTS` | paper already in the reading list |
| 409 | `INVALID_STATUS_TRANSITION` | status change not allowed from the current status |
| 405 | `METHOD_NOT_ALLOWED` | HTTP method not supported |
| 409 | `EMAIL_ALREADY_EXISTS` | email registered (case-insensitive) |
| 409 | `CUSTOMER_PROFILE_ALREADY_EXISTS`, `STUDENT_PROFILE_ALREADY_EXISTS` | one profile per user |
| 409 | `STUDENT_NUMBER_ALREADY_EXISTS` | number already used at that university |
| 409 | `CONFLICT` | concurrent write hit a unique constraint |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | send `Content-Type: application/json` |
| 500 | `INTERNAL_ERROR` | unexpected; details only in the server log (find it by `X-Request-Id`) |
