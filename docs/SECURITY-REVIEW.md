# Security review

Scope: backend, AI engine, student platform, corporate website and the Docker/Compose setup. Method: reading the code and configuration against a checklist (OWASP-style: authentication, access control, input handling, secrets, data protection, configuration, dependencies), plus automated tests and static checks listed below. **No penetration test and no dependency vulnerability scan has been done.** Findings are ordered by what to fix first.

## 1. What is in place

| Area | Control | Where / how verified |
|------|---------|----------------------|
| Passwords | BCrypt hashes; never returned or logged; 8 to 72 characters | `UserService`, tests `AuthApiTest`, `ApplicationContextTest` |
| Login | same generic error for wrong password, unknown e-mail and disabled account; dummy hash check against timing differences | `AuthService`, `AuthApiTest` |
| Tokens | HS256 JWT, secret from environment (32+ chars, startup fails otherwise), expiry checked, tampered/foreign/expired tokens rejected | `JwtService`, `JwtServiceTest`, `SecurityRulesTest` |
| Account state | account reloaded from the database on every request: disabled accounts and role changes take effect immediately; the role is never trusted from the token | `JwtAuthenticationFilter`, `SecurityRulesTest` |
| Access control | URL rules by role (`/customer/**`, `/student/**`, `/admin/**`); ownership derived from the token; other users' items answer 404; no user id accepted from clients | `SecurityConfig`, `ProfileOwnershipTest`, every `*ApiTest` |
| Privilege escalation | registration cannot create ADMIN; mass assignment prevented by DTOs (extra JSON fields ignored) | `AuthApiTest`, `BasicProfileApiTest` |
| Brute force / spam | per-client limits on login, registration and guest inquiries (429, `Retry-After`); research analysis once per minute per project | `RateLimitFilter`, `RateLimitTest`, `RateLimitFilterIntegrationTest`, `ResearchApiTest` |
| Input validation | Jakarta Validation on every request body, enums, length limits, http(s)-only links, request size limits | DTOs, `GlobalExceptionHandlerTest`, `ResearchApiTest` |
| SQL injection | Spring Data / JPA parameter binding only, no string-built SQL | code review, `grep` in `scripts/verify-consistency.py` is not applicable; no native queries exist |
| File uploads | extension allow-list (no executables, scripts, HTML/SVG, archives), 10 MB limit, random storage names, sanitized display names, path-traversal check, content type from the extension table, `attachment` + `nosniff` on download | `DocumentFileTypes`, `LocalDocumentStorage`, `ProjectDocumentApiTest`, unit tests |
| Error handling | one JSON format; no stack traces, SQL or request values in responses; unexpected errors are generic 500s | `GlobalExceptionHandler`, `GlobalExceptionHandlerTest` |
| Secrets | environment variables only; `.env` git-ignored; compose refuses to start without required secrets; test secrets are clearly marked | `ConfigurationSecretsTest`, `verify-consistency.py` |
| Logging | request id on every line; no passwords, tokens, keys, bodies or client addresses | filters and services; reviewed |
| Browser side | token in httpOnly SameSite=Strict cookie (student platform); no `localStorage` use; no `dangerouslySetInnerHTML`; external links only http(s) and `rel="noopener noreferrer"`; React escapes output; CSRF: SameSite=Strict cookie plus JSON-only APIs | `verify-consistency.py`, `safe-url.test.ts` |
| BFF proxy | allow-list of backend paths, traversal blocked, login/register not proxied, non-student accounts refused | `proxy-path.test.ts`, smoke test |
| Security headers | Spring Security defaults on the API; `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy` on both Next apps | `next.config.ts`, `SecurityConfig` |
| CORS | explicit origins from configuration, no wildcard, no credentials | `CorsConfig`, `SecurityRulesTest` |
| AI engine | shared-secret API key with constant-time comparison; only key terms leave to external sources; key never logged; interactive docs off by default; internal network only | `security.py`, `test_core.py`, compose |
| Actuator | only `health` (and `info` in dev); health details hidden in production | `application-prod.yml` |
| Containers | non-root users in all images; secrets passed at run time, not baked in | Dockerfiles |
| Database | constraints enforce uniqueness, ownership links and valid values independent of the application; `ON DELETE RESTRICT`; destructive SQL forbidden by a test; `flyway clean` disabled | migrations, `*ConstraintsTest`, `MigrationSafetyTest` |

## 2. Known gaps, by priority

### Fix before real users
1. **No password reset, change password or e-mail verification.** A forgotten password cannot be recovered, and nothing proves that a registrant owns the e-mail address. Also needed for account recovery and for trusting the e-mail address in inquiries.
2. **No token revocation or refresh.** A stolen token works until it expires (default 24 h). Mitigations that exist: disabling the account stops it at once; shorter `JWT_EXPIRATION`. "Sign out" in the student platform deletes the cookie but does not invalidate the token on the server.
3. **First admin is set by SQL and there is no audit trail** of admin actions (status changes are logged with ids only). Add an audit table before more staff use the admin endpoints.
4. **HTTPS is not part of the compose file.** Use the proxy add-on (untested) or your own proxy; add HSTS (done in the Caddyfile example) and consider a Content-Security-Policy there.
5. **No dependency vulnerability scanning.** Add `mvn org.owasp:dependency-check-maven:check`, `npm audit` and `pip-audit` to a CI pipeline, and pin/lock dependencies (no lock files were generated: the authoring environment had no network).
6. **Backups and restore are documented but untested** (see `docs/DEPLOYMENT.md`).

### Should fix soon
7. **Rate limits and the AI cache are per instance and in memory.** Fine for one instance; move to a shared store before scaling out. Client addresses are only reliable behind a trusted proxy (`FORWARD_HEADERS_STRATEGY=framework` must only be set when the proxy overwrites `X-Forwarded-For`).
8. **Uploaded files are not scanned and only checked by extension.** A renamed file passes. Add magic-number checks and an antivirus scan (for example ClamAV) before accepting uploads from the public; keep downloads as attachments.
9. **Shared static key between backend and AI engine, plain HTTP inside the Docker network.** Acceptable on a private network; use TLS or a service mesh if the engine runs on another host, and rotate the key by redeploying both.
10. **The AI engine calls external sites** with student-derived key terms. Disclosed in the interface text; reflect it in the privacy notice. The engine has no outbound allow-list beyond its three fixed base URLs.
11. **Guest inquiries have no captcha or e-mail confirmation**; the rate limit is the only barrier. Staff UIs that show inquiry text later must escape it (the API stores raw text).
12. **Account enumeration at registration**: `EMAIL_ALREADY_EXISTS` reveals whether an address is registered (login does not). Usual trade-off; fix with e-mail verification.

### Accepted for now
13. Passwords may be weak beyond the length rule (no breached-password check).
14. The JWT is HS256 with one shared secret; asymmetric keys would allow other services to verify tokens without being able to mint them.
15. Document names and descriptions are stored as given (after sanitising path parts); they are only ever rendered by React.
16. Personal data handling (retention, export, deletion) is a policy decision, see `docs/DEPLOYMENT.md` section 7.

## 3. How to re-check

```bash
python3 scripts/verify-consistency.py        # static cross-checks (secrets, forbidden patterns, contracts)
cd backend && mvn clean test                 # security, ownership and constraint tests
cd ai-service && pytest
scripts/smoke-test.sh                        # live isolation and role checks against a running system
```
After the first successful run, schedule the dependency scans from gap 5 and repeat this review whenever authentication, upload or proxy code changes.
