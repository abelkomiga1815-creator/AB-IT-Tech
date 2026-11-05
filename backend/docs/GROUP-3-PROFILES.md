# GROUP 3 — User, Customer and Student Profiles

## 1. User vs Profile architecture

```text
                    USER
                     |
          +----------+----------+
          |                     |
          v                     v
 CUSTOMER PROFILE        STUDENT PROFILE
          |                     |
          v                     v
Customer Services       Student Projects
                              |
                              v
                         Research / AI
```

| Concern | Lives in |
|---------|----------|
| Authentication: id, fullName, email, password hash, role, enabled, timestamps | `users` / `User` |
| Customer information | `customer_profiles` / `CustomerProfile` |
| Student information | `student_profiles` / `StudentProfile` |

Profiles never duplicate `fullName`, `email`, `password` or `role`. Profiles are not created automatically at registration; a later registration workflow can create the right one.

## 2. Customer profile

Fields: `id`, `customerType`, `organizationName`, `phone`, `address`, `city`, `country`, `createdAt`, `updatedAt` (the owning user is implicit).

Customer types (enum `CustomerType`, stored as text with a database check): `INDIVIDUAL`, `BUSINESS`, `ORGANIZATION`, `INSTITUTION`, `STARTUP`, `OTHER`.

## 3. Student profile

Fields: `id`, `university`, `programme`, `yearOfStudy`, `studentNumber`, `phone`, `createdAt`, `updatedAt`.

`university` and `programme` are plain text. They may later become normalised entities if the platform needs university management; that would be a new migration, not part of this task.

## 4. Entity relationships

```text
users 1 ---- 0..1 customer_profiles   (customer_profiles.user_id UNIQUE, FK -> users.id, ON DELETE RESTRICT)
users 1 ---- 0..1 student_profiles    (student_profiles.user_id  UNIQUE, FK -> users.id, ON DELETE RESTRICT)
```

## 5. API endpoints

| Method | Path | Role | Success |
|--------|------|------|---------|
| GET | `/api/v1/profile` | any authenticated | 200 |
| PUT | `/api/v1/profile` | any authenticated | 200 |
| GET | `/api/v1/customer/profile` | CUSTOMER | 200 |
| POST | `/api/v1/customer/profile` | CUSTOMER | 201 |
| PUT | `/api/v1/customer/profile` | CUSTOMER | 200 |
| GET | `/api/v1/student/profile` | STUDENT | 200 |
| POST | `/api/v1/student/profile` | STUDENT | 201 |
| PUT | `/api/v1/student/profile` | STUDENT | 200 |

PUT on a profile replaces all editable fields (send the full set). Examples: `docs/api/API-REFERENCE.md`.

## 6. Security rules

- Every profile endpoint requires a valid JWT (missing/invalid -> 401).
- The profile is resolved from the authenticated user id; there is no `{userId}` in any URL and ids in a request body are ignored. A user therefore cannot read or change someone else's profile (covered by `ProfileOwnershipTest`).
- Email, role, enabled, password and id cannot be changed through `PUT /profile`: the request DTO only declares `fullName`.
- Responses never contain the password hash or other accounts' data. Logs record events (created/updated, user id) but never personal field values, passwords or tokens.

## 7. Role rules

| Role | May have |
|------|----------|
| STUDENT | `StudentProfile` |
| CUSTOMER | `CustomerProfile` |
| ADMIN | neither; admin access to profiles is a future `/api/v1/admin/**` feature |

Role-to-URL rules are in `SecurityConfig`. An ADMIN calling `/customer/profile` or `/student/profile` receives 403 because those endpoints are for the owner only.

## 8. Database migrations

- `V3__create_customer_profiles.sql`, `V4__create_student_profiles.sql` (new files; V1 and V2 are untouched).
- Constraints: primary key, `NOT NULL` where required, `UNIQUE(user_id)`, foreign key to `users`, check constraints for enums and year range, `UNIQUE(university, student_number)`.
- **Delete strategy:** `ON DELETE RESTRICT`. A user that still has a profile cannot be deleted, so data is never removed by an unexpected cascade. Accounts are disabled (`enabled = false`) rather than deleted; a deliberate, audited deletion procedure (profile first, then user) can be added later if legally required.

## 9. Validation

Customer: `customerType` required; `organizationName` required for BUSINESS, ORGANIZATION, INSTITUTION and STARTUP, optional for INDIVIDUAL and OTHER; `phone` optional but must look like a phone number; `address` up to 300, `city`/`country` up to 100, `organizationName` up to 200 characters; blank optional values are stored as empty.

Student: `university` and `programme` required (max 200); `yearOfStudy` optional, 1–10; `studentNumber` optional (not every student has one), letters/digits and `/ . _ -`, unique per university (case-insensitive); `phone` optional with the same rule as for customers.

## 10. Error handling

All errors use the global format `{success:false, code, message, timestamp}`.

| Situation | HTTP | code |
|-----------|------|------|
| No/invalid token | 401 | `UNAUTHORIZED` |
| Wrong role | 403 | `FORBIDDEN` |
| Profile missing | 404 | `CUSTOMER_PROFILE_NOT_FOUND` / `STUDENT_PROFILE_NOT_FOUND` |
| Profile exists already | 409 | `CUSTOMER_PROFILE_ALREADY_EXISTS` / `STUDENT_PROFILE_ALREADY_EXISTS` |
| Student number taken | 409 | `STUDENT_NUMBER_ALREADY_EXISTS` |
| Field-level validation | 400 | `VALIDATION_ERROR` (+ `errors[]`) |
| Business rule (e.g. organisation name missing) | 400 | `INVALID_PROFILE_DATA` |
| Race on a unique constraint | 409 | `CONFLICT` |

Database exceptions are never exposed.

## 11. Future extension possibilities

- Customer inquiries and service requests referencing `customer_profiles`.
- Projects, progress, documents, research and defense preparation referencing `student_profiles`.
- Normalised `universities` / `programmes` tables.
- Admin endpoints under `/api/v1/admin/**` to list and manage profiles.
- A registration workflow that creates the matching profile in one step.
