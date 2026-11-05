# Backend architecture

## Request flow

```text
HTTP request
   -> RequestIdFilter            adds X-Request-Id, logs method/path/status/time
   -> Spring Security chain      CORS -> JwtAuthenticationFilter -> URL authorization
   -> Controller                 HTTP only, @Valid DTOs, @AuthenticationPrincipal
   -> Service                    business rules, transactions
   -> Repository (Spring Data)   persistence
   -> PostgreSQL (Flyway-managed schema)

Errors: GlobalExceptionHandler (inside MVC) or RestErrorWriter (inside security filters)
        -> ApiErrorResponse {success:false, code, message, timestamp}
```

## Packages (feature folders, layers inside)

| Package | Responsibility |
|---------|----------------|
| `config` | `JwtProperties`, `CorsProperties`, `CorsConfig`, `ClockConfig` |
| `security` | `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `AuthenticatedUser`, `RestAuthenticationEntryPoint` (401), `RestAccessDeniedHandler` (403) |
| `common.exception` | `ApiException`, `ErrorCode`, `GlobalExceptionHandler` |
| `common.response` | `ApiResponse<T>`, `ApiErrorResponse`, `FieldValidationError` |
| `common.logging` | `RequestIdFilter` |
| `common.persistence` | `AuditedEntity` (UUID id, created/updated timestamps) |
| `user` | `User`, `Role`, `UserRepository`, `UserService` (email normalisation, hashing) |
| `auth` | register / login / me |
| `profile` | `GET/PUT /api/v1/profile` (fullName only) |
| `customer`, `student` | profile entity, repository, service, DTOs, controller |
| `service` | service catalogue (`ServiceOffering`, read-only API) |
| `inquiry` | `CustomerInquiry`: guest, customer and admin endpoints |
| `servicerequest` | `ServiceRequest` with `ServiceRequestStatus` lifecycle: customer and admin endpoints |
| `project` | `Project`, `ProjectProgress`; `ProjectService.requireOwned` is the single ownership check used by progress, documents and defense questions |
| `document` | `ProjectDocument`, `DocumentService`, `LocalDocumentStorage`, `DocumentFileTypes` (whitelist, filename sanitising) |
| `defense` | `DefenseQuestion` and its service/controller |

## Key decisions

1. **Account and profile are separate.** `users` is only for authentication. Profiles hang off it with a unique foreign key.
2. **Owner from the token.** Services receive the authenticated user id; no endpoint accepts a user id for ordinary operations.
3. **Account re-checked per request.** `JwtAuthenticationFilter` loads the user from the database, so disabling an account or changing a role takes effect immediately. The role is never read from the token.
4. **Role rules in one place.** `SecurityConfig` maps `/customer/**` -> CUSTOMER, `/student/**` -> STUDENT and `/admin/**` -> ADMIN; the catalogue (GET) and the guest inquiry (POST) are public.
5. **Database enforces integrity.** Unique, foreign-key and check constraints exist in the migrations; the application also pre-checks to give friendly error codes.
6. **One response format.** `ApiResponse` for success and `ApiErrorResponse` for failures, including 401/403.
7. **No destructive deletes.** Profile foreign keys use `ON DELETE RESTRICT`; migrations never drop or truncate.
