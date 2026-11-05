# Deployment

## 1. Everything on one machine (Docker Compose)

Requirements: Docker with Compose v2, about 4 GB of free memory for the first build.

```bash
cp .env.example .env     # set DATABASE_PASSWORD, JWT_SECRET, AI_SERVICE_API_KEY (and optionally CONTACT_EMAIL)
docker compose up -d --build
docker compose ps        # all services should become "healthy" / "running"
scripts/smoke-test.sh
```

Generate secrets with `openssl rand -base64 48` (JWT secret and AI key must be different values, 32+ characters each).

| URL | |
|-----|-|
| http://localhost:3000 | corporate website |
| http://localhost:3001 | student platform |
| http://localhost:8080/api/v1/health | backend API |

Stop: `docker compose down` (data is kept in the `pgdata` and `documents` volumes). **`docker compose down -v` deletes the data.**

Note: the website's `NEXT_PUBLIC_*` addresses are compiled into its code at build time. After changing `PUBLIC_API_URL`, `CONTACT_*` and similar values run `docker compose up -d --build corporate-website`.

## 2. First administrator

There is no admin registration (by design). Register a normal account, then promote it directly:

```bash
docker compose exec postgres psql -U "$DATABASE_USERNAME" -d abittech \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.org';"
```

Log in again to get a token with the new role (the role is read from the database on every request, so existing tokens also work). Admin endpoints: `/api/v1/admin/inquiries` and `/api/v1/admin/service-requests`.

## 3. Production

1. **Server and DNS.** One Linux server with Docker; three DNS names pointing to it (website, student platform, API).
2. **HTTPS reverse proxy.** `cp deploy/Caddyfile.example deploy/Caddyfile`, put your domain names in it, then:
   ```bash
   docker compose -f docker-compose.yml -f deploy/docker-compose.proxy.yml up -d --build
   ```
   This exposes only ports 80/443 (Caddy gets certificates automatically), removes the localhost port mappings, and sets `FORWARD_HEADERS_STRATEGY=framework` so the backend sees real client addresses for rate limiting. Needs Compose 2.24+. This add-on has not been tested.
3. **Set the public addresses in `.env`** to the HTTPS names: `PUBLIC_API_URL=https://api...`, `PUBLIC_WEBSITE_URL`, `PUBLIC_STUDENT_URL`, and `CORS_ALLOWED_ORIGINS=https://<website domain>` (exactly the website origin, no trailing slash). Rebuild the website.
4. **Secrets**: only in `.env` on the server (permissions `600`), never in git. Rotate `JWT_SECRET` to log everybody out; use a different `AI_SERVICE_API_KEY` from `JWT_SECRET`.
5. **Do not publish** PostgreSQL (5432) or the AI engine (8000).
6. **Firewall**: allow 22 (SSH, keys only), 80, 443.

### Environment variables (summary)

| Variable | Used by | Meaning |
|----------|---------|---------|
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | postgres, backend | database account |
| `JWT_SECRET`, `JWT_EXPIRATION` | backend | token signing secret (32+ chars) and lifetime in ms |
| `AI_SERVICE_API_KEY` | backend, ai-service | shared secret (32+ chars) |
| `CORS_ALLOWED_ORIGINS` | backend | browser origins allowed to call the API (the website) |
| `PUBLIC_API_URL`, `PUBLIC_WEBSITE_URL`, `PUBLIC_STUDENT_URL` | website build, student build | addresses as browsers see them |
| `CONTACT_EMAIL`, `CONTACT_PHONE`, `CONTACT_ADDRESS` | website build; AI engine (`CONTACT_EMAIL`) | company contact details |
| `SEMANTIC_SCHOLAR_API_KEY` | ai-service | optional, higher limits |
| `RATE_LIMIT_AUTH_PER_MINUTE`, `RATE_LIMIT_INQUIRY_PER_MINUTE`, `FORWARD_HEADERS_STRATEGY`, `DOCUMENTS_DIR` | backend | optional tuning (see `backend/README.md`) |

## 4. Backups and restore

Two things hold data: the **database** and the **documents volume**. Back them up together.

```bash
# backup (run daily from cron; copy the files off the server)
docker compose exec -T postgres pg_dump -U "$DATABASE_USERNAME" -Fc abittech > backup-$(date +%F).dump
docker run --rm -v abittech_documents:/data -v "$PWD":/backup alpine tar czf /backup/documents-$(date +%F).tgz -C /data .

# restore into an empty database
docker compose exec -T postgres pg_restore -U "$DATABASE_USERNAME" -d abittech --clean --if-exists < backup-YYYY-MM-DD.dump
docker run --rm -v abittech_documents:/data -v "$PWD":/backup alpine tar xzf /backup/documents-YYYY-MM-DD.tgz -C /data
```

Test a restore on a spare machine before you need it. The volume name is `<project>_documents`; check with `docker volume ls`.

## 5. Upgrading

1. Back up (section 4). 2. `git pull` (or copy the new version). 3. `docker compose up -d --build`.
Flyway applies new migrations when the backend starts. Migrations are never edited after release; if a migration fails the backend does not start, and the database is unchanged by the failed statement only if PostgreSQL could roll it back (DDL is transactional in PostgreSQL), so restore the backup if in doubt.

## 6. Operations

- Health: `GET /api/v1/health` (liveness), `GET /actuator/health` (includes the database), AI engine `GET /health` (internal).
- Logs: `docker compose logs -f backend`. Every log line carries a request id, and every API response has an `X-Request-Id` header, so one report can be traced. Logs hold paths, status codes and timings, not passwords, tokens or request bodies.
- Troubleshooting tables: `backend/docs/troubleshooting/TROUBLESHOOTING.md`.
- Scaling limits: the rate limiter and the AI engine's cache are in memory per instance; run one backend instance until they are moved to a shared store (for example Redis). Uploaded documents live on one disk.
- Costs and external limits: each analysis makes up to 9 requests to external sources; the backend allows one analysis per project per minute.

## 7. Data protection

The system stores personal data (names, e-mail addresses, university, student numbers, uploaded documents). Before going live: publish a privacy notice, decide how long data is kept, define how a person can ask for their data to be exported or deleted (accounts are disabled, not deleted, by design; real deletion needs a reviewed procedure), and check the data-protection law that applies to you. This document does not give legal advice.
