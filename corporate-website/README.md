# AB IT TECH SOLUTIONS — Corporate Website (GROUP 1)

Public website for **all** customers of AB IT TECH SOLUTIONS (individuals, businesses, organizations, institutions, startups, students and others). Built with **Next.js (App Router) and TypeScript**. It is separate from the Student Project Platform (GROUP 2), which it only promotes.

## Pages

| Route | Page |
|-------|------|
| `/` | Home |
| `/about` | About |
| `/services` | All services |
| `/services/[slug]` | One page per service (7 pages) |
| `/projects` | Portfolio (honest empty state until real projects are added) |
| `/student-platform` | Promotes the Student Platform, links to GROUP 2 |
| `/contact` | Contact form and details |
| `/get-started` | Request a service (`?service=<slug>` pre-selects one) |

The seven services are exactly: Software Development, Web Development, Mobile App Development, Cybersecurity, Data Analysis, UI/UX & Graphic Design, IT Support & Maintenance (`src/data/services.ts`, protected by a test).

## Requirements

Node.js 20+ and npm.

## Run

```bash
cd corporate-website
cp .env.example .env.local     # then edit the values
npm install
npm run dev                    # http://localhost:3000
```

Other scripts: `npm run build`, `npm start`, `npm run lint`, `npm run typecheck`, `npm test`.

## Configuration (`.env.local`)

| Variable | Purpose |
|----------|---------|
| `NEXT_PUBLIC_SITE_URL` | public URL (metadata, sitemap) |
| `NEXT_PUBLIC_API_BASE_URL` | Spring Boot backend, e.g. `http://localhost:8080` |
| `NEXT_PUBLIC_STUDENT_PLATFORM_URL` | where the Student Platform runs |
| `NEXT_PUBLIC_CONTACT_EMAIL`, `_PHONE`, `_ADDRESS` | contact details shown on the site; nothing is shown if empty |

`NEXT_PUBLIC_*` values are visible in the browser, so never put secrets here. The backend must allow this site's origin in its `CORS_ALLOWED_ORIGINS` once the site calls the API.

## Logo

The official logo has not been supplied, so `public/logo-placeholder.svg` (clearly labelled "OFFICIAL LOGO PLACEHOLDER") is used. To switch: copy the official file into `public/`, then set `LOGO_SRC` and `LOGO_IS_PLACEHOLDER = false` in `src/config/site.ts`. Colours are CSS variables at the top of `src/app/globals.css`; adjust them to match the logo.

## Forms and the backend (updated)

The contact and request forms now send valid requests to `POST /api/v1/inquiries` on the backend (`NEXT_PUBLIC_API_BASE_URL`). The backend must list this site's origin in `CORS_ALLOWED_ORIGINS`. If sending fails and `NEXT_PUBLIC_CONTACT_EMAIL` is set, the visitor is offered the same request by e-mail. The service list on this site is static and is checked against the backend's catalogue by `scripts/verify-consistency.py`.

### History (earlier behaviour)

The backend has no inquiry API yet (it arrives in the customer-services stage). Until then, a valid request opens the visitor's email app addressed to `NEXT_PUBLIC_CONTACT_EMAIL`; with no email configured the form is disabled and says so. All backend calls will go through `src/lib/api.ts` (typed client with the standard `ApiResponse` / error format); pages contain no `fetch` calls.

## Structure

```text
corporate-website/
├── public/logo-placeholder.svg
└── src/
    ├── app/            routes, layout, global CSS, sitemap, robots
    ├── components/     Header, Footer, Logo, Icon, ServiceCard, PageHero, InquiryForm
    ├── config/site.ts  name, nav, contact, URLs, logo
    ├── data/           services.ts (7 agreed services), projects.ts (empty until approved)
    └── lib/            api.ts (typed backend client), inquiry.ts (validation, mailto)
```

## Quality notes

Responsive layout, skip link, semantic landmarks, visible focus, labelled form fields with error messages, reduced-motion support. No invented clients, statistics or testimonials.

## Status

Written without running `npm install` or the build in the authoring environment (no network there). Run `npm install && npm run typecheck && npm run lint && npm test && npm run build` first and report any errors.
