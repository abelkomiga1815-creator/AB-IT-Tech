# GROUP 3 — Customer Services (catalogue, inquiries, service requests)

## 1. Purpose

Lets the corporate website (GROUP 1) show the company's services, receive inquiries from anyone, and lets registered customers send inquiries and request services and follow their progress. It builds on the customer profile from the profiles task.

```text
                         services (7 agreed, read-only API)
                           |                |
                           v                v
 guest (no login) --> customer_inquiries   service_requests <-- customer (login + profile)
                           ^                         ^
                           |                         |
 customer (login + profile)+---- customer_profiles --+
                                       ^
                                      users
 staff (ADMIN) reads and updates inquiries and service requests
```

## 2. Service catalogue

Table `services`, seeded by migration V5 with exactly: Software Development, Web Development, Mobile App Development, Cybersecurity, Data Analysis, UI/UX & Graphic Design, IT Support & Maintenance. Slugs match the website (`software-development`, ... `it-support-maintenance`). `GET /api/v1/services` and `GET /api/v1/services/{slug}` are public and show active services only. There is no write API: change the catalogue with a new Flyway migration (or set `active = FALSE` to hide a service). Services are never deleted because inquiries and requests reference them (`ON DELETE RESTRICT`).

## 3. Inquiries

| Who | Endpoint | Notes |
|-----|----------|-------|
| Anyone | `POST /api/v1/inquiries` | Guest inquiry. Always anonymous: a token, if sent, is ignored, so identity is never guessed. Response only confirms id/status/time. |
| CUSTOMER | `POST /api/v1/customer/inquiries` | Linked to the customer's profile. Name, email and phone are taken from the account and profile, never from the request. |
| CUSTOMER | `GET /api/v1/customer/inquiries[/{id}]` | Own inquiries only; someone else's id answers 404. |
| ADMIN | `GET /api/v1/admin/inquiries[/{id}]`, `PUT .../{id}/status` | Paged list (filter by status), details with contact data, set status NEW / IN_PROGRESS / RESOLVED. |

Why a customer profile is required for customer inquiries: the inquiry belongs to the customer profile (the relationship the later modules build on). Visitors without an account use the guest endpoint.

## 4. Service requests (the customer side "project")

A customer profile has many service requests. A request references one service and has a `title`, `description` and `status`.

```text
SUBMITTED -> IN_REVIEW -> ACCEPTED -> IN_PROGRESS -> COMPLETED
   |            |            |             |
   +-> REJECTED +-> REJECTED +-> CANCELLED +-> CANCELLED     (SUBMITTED and IN_REVIEW can also be CANCELLED)
```

- Customers create requests, read their own, and cancel while `SUBMITTED` or `IN_REVIEW`.
- Only ADMIN moves a request through the lifecycle. Skipping steps or leaving a final status gives `409 INVALID_STATUS_TRANSITION`. The rules are in `ServiceRequestStatus` and unit-tested.

## 5. Minimal admin access

`/api/v1/admin/**` (role ADMIN only) exists for exactly two reasons: guest inquiries would otherwise be unreadable, and customers cannot progress their own requests. It is not a dashboard: no users, profiles or catalogue management. The first admin is promoted directly in the database (see `docs/database/SCHEMA.md`).

## 6. Security rules

- Public: `GET /services/**` and `POST /inquiries`. Everything else needs a JWT.
- `/customer/**` -> CUSTOMER, `/admin/**` -> ADMIN (in `SecurityConfig`). A student or admin on a customer URL gets 403.
- Ownership is derived from the authenticated user: customers cannot read, cancel or list other customers' items, and no endpoint accepts a customer id.
- Responses to guests contain no personal data. Logs contain ids and statuses, never names, emails, phone numbers or message text.

## 7. Database

V5 `services`, V6 `customer_inquiries`, V7 `service_requests` (see `docs/database/SCHEMA.md`). Foreign keys use `ON DELETE RESTRICT`, status columns have check constraints, and lookups are indexed.

## 8. Validation

Guest inquiry: `name` (max 150), valid `email`, optional valid `phone`, optional `serviceSlug`, `message` 10-4000 characters. Customer inquiry: `message` 10-4000, optional `serviceSlug`. Service request: `serviceSlug`, `title` (max 200), `description` 10-4000. Emails are stored lower-case. Unknown/inactive services give `SERVICE_NOT_FOUND`.

## 9. Limitations and next steps

- The public inquiry endpoint has no rate limiting or captcha yet; add one before exposing it widely.
- No email notifications to staff or customers.
- Staff cannot reply inside the system; they use the contact details on the inquiry.
- The website (GROUP 1) still opens the visitor's email app; it will call `POST /api/v1/inquiries` and `GET /api/v1/services` during the integration stage (the backend must list the website origin in `CORS_ALLOWED_ORIGINS`).
- Student projects (GROUP 2) are a separate stage and use `student_profiles`, not service requests.
