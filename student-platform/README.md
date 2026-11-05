# AB IT TECH SOLUTIONS — Student Project Platform (GROUP 2)

Web app for university and college students (Next.js App Router + TypeScript). It is a **separate product** from the corporate website (GROUP 1) and talks only to the Spring Boot backend (GROUP 3).

## Features

| Area | What the student can do |
|------|-------------------------|
| Account | register (always a STUDENT account), sign in, sign out |
| Profile | change name; create/edit the student profile (university, programme, year, student number, phone) |
| Dashboard | project count, average progress, next deadline, recent projects |
| Projects | create, list, edit (title, description, research area, supervisor, deadline, status) |
| Progress | record a stage and completion percentage with a note; see the history |
| Documents | upload (pdf, doc(x), ppt(x), xls(x), csv, txt, md, png, jpg; max 10 MB), download, delete |
| Defense preparation | write expected questions, prepare answers, mark them as prepared |
| Research assistance | run an analysis of the project (related papers, similarity indicator, gap suggestions with evidence, title ideas), read earlier analyses, save papers to a reading list |

Research results come from the AI engine through the backend; the page shows which sources answered, keeps retrieved facts apart from the tool's interpretation, and always shows the disclaimer that the indicator is not a plagiarism check.

## Requirements

Node.js 20+, npm, and the backend (GROUP 3) running, by default on `http://localhost:8080`.

## Run

```bash
cd student-platform
cp .env.example .env.local     # set BACKEND_URL if the backend is not on localhost:8080
npm install
npm run dev                    # http://localhost:3001
```

Other scripts: `npm run build`, `npm start`, `npm run lint`, `npm run typecheck`, `npm test`.

## Configuration

| Variable | Purpose |
|----------|---------|
| `BACKEND_URL` | Spring Boot base URL. Server-side only (not exposed to the browser). |
| `NEXT_PUBLIC_CORPORATE_SITE_URL` | link back to the corporate website |

## How authentication works

```text
Browser --(same origin)--> Next.js route handlers --(Bearer token)--> Spring Boot API
          /api/auth/login|register|logout     sets/clears an httpOnly session cookie
          /api/backend/...                    adds the Authorization header from the cookie
```

- The JWT is stored in an **httpOnly, SameSite=Strict cookie** (`Secure` in production). Browser JavaScript never sees the token, so script injection cannot steal it.
- Because the browser only talks to its own origin, **no CORS configuration is needed** on the backend for this app.
- `/api/backend/[...path]` only forwards allow-listed areas (`auth/me`, `profile`, `student/**`, `services`); login/registration, customer and admin endpoints are not reachable through it (`src/lib/proxy-path.ts`, unit-tested).
- Registration always creates a STUDENT account (the role is set on the server), and non-student accounts cannot sign in here.
- `middleware.ts` redirects visitors without a session cookie to `/login`. The backend remains the real authority: every API call is re-checked there, and students can only ever reach their own data.
- Production: serve over HTTPS (the cookie is `Secure`) and set `BACKEND_URL` to the internal backend address.

## Structure

```text
student-platform/
└── src/
    ├── app/
    │   ├── page.tsx                      landing
    │   ├── (auth)/login, register
    │   ├── (app)/dashboard, profile, projects, projects/[id]  (details, progress, documents, defense panels)
    │   └── api/auth/*, api/backend/[...path]                  server-side route handlers
    ├── components/                       AppNav, Alert, ProgressBar, StatusBadge, Loading
    ├── config/site.ts
    ├── lib/                              api (client), useLoad, constants, types, format, upload, proxy-path, server/backend
    └── middleware.ts
```

## Tests

`npm test` runs unit tests for the proxy allow-list, formatting helpers, upload pre-checks and the enum lists (which must match the backend enums).

## Limitations

- No password reset, email verification or "change password" yet (backend features of a later task).
- Documents live on the backend server's disk; there is no virus scanning.
- No offline support, no real-time updates, no notifications.
- Docker: `docker build` uses `output: "standalone"`; see the repository `docker-compose.yml`.
- Written without running `npm install`/`next build` in the authoring environment (no network). Run `npm install && npm run typecheck && npm run lint && npm test && npm run build` first and report any errors.
