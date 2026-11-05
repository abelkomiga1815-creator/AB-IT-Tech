# AB IT TECH SOLUTIONS — AI Research & Innovation Engine (GROUP 4)

Python / FastAPI service that helps students understand where their project sits in the literature. It is called by the Spring Boot backend (GROUP 3); it has no database and keeps no user data.

## What it does

| Endpoint | Purpose |
|----------|---------|
| `POST /api/v1/research/keywords` | key terms and the search queries derived from them (no external calls) |
| `POST /api/v1/research/related-work` | related papers from OpenAlex, Crossref and Semantic Scholar, with the status of each source |
| `POST /api/v1/research/similarity` | similarity **indicator** against the retrieved papers |
| `POST /api/v1/research/gaps` | evidence-based research gap suggestions |
| `POST /api/v1/research/titles` | alternative title ideas and terms common in related papers |
| `POST /api/v1/research/report` | everything above as one structured JSON report |
| `POST /api/v1/research/report/markdown` | the same report as Markdown |
| `GET /health` | liveness (public) |

Request body (all research endpoints): `{"title": "...", "abstract": "optional", "maxResults": 10}` (`title` 3-300 characters, `abstract` up to 5000, `maxResults` 1-20 per source).
Responses use the same envelope as the Spring Boot API: `{"success": true, "data": ...}` or `{"success": false, "code", "message", "timestamp"}`.

## Principles

- **Never claims originality.** The similarity result is an indicator (`low` / `moderate` / `high` / `insufficient_data`) with a fixed disclaimer. "No papers found" is reported as `insufficient_data`, not as "original".
- **Facts and interpretation are separate.** The report has a `retrieved` part (papers, links, source statuses) and an `analysis` part (the tool's own interpretation).
- **Evidence for every gap suggestion**: counts, example papers with links, or limitation sentences quoted from abstracts. Confidence is `low` or `medium`, never `high`, and a caveat is always attached.
- **Honest about failures.** Each source reports `ok`, `no_results`, `rate_limited` or `unavailable`. "The source could not be reached" is never confused with "nothing exists". One failing source does not hide the others, and partial results are kept.
- **Privacy.** Only extracted key terms are sent to the external sources, never the student's title or abstract as a whole. Nothing is stored; logs contain paths, status codes and timings only, never bodies or keys.

## Requirements

Python 3.11+ (developed on 3.12).

## Run

```bash
cd ai-service
python -m venv .venv && source .venv/bin/activate     # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env        # then set AI_SERVICE_API_KEY (min. 32 characters) - see below
export $(grep -v '^#' .env | xargs)                     # or set the variables your own way
uvicorn app.main:create_app --factory --port 8000
```

The service refuses to start without `AI_SERVICE_API_KEY`. In the full system it is started by `docker-compose.yml` and only the backend calls it. Try it:

```bash
curl -s localhost:8000/health
curl -s -X POST localhost:8000/api/v1/research/keywords \
  -H "X-API-Key: $AI_SERVICE_API_KEY" -H "Content-Type: application/json" \
  -d '{"title":"Crop disease detection on smartphones","abstract":"A mobile app that detects crop disease from leaf images."}'
```

Docker: `docker build -t abittech-ai . && docker run --env-file .env -p 8000:8000 abittech-ai`.

## Configuration

| Variable | Required | Meaning |
|----------|----------|---------|
| `AI_SERVICE_API_KEY` | yes (>= 32 chars) | shared secret; callers send it as `X-API-Key`. Use the same value in the Spring Boot backend's configuration. |
| `CONTACT_EMAIL` | recommended | sent to OpenAlex/Crossref ("polite pool": more reliable access) |
| `SEMANTIC_SCHOLAR_API_KEY` | no | higher Semantic Scholar rate limits |
| `PROVIDER_TIMEOUT_SECONDS` | no (10) | per-source timeout |
| `CACHE_TTL_SECONDS`, `CACHE_MAX_ENTRIES` | no (3600, 500) | in-memory cache of source answers |
| `AI_SERVICE_LOG_LEVEL` | no (INFO) | |
| `AI_SERVICE_ENABLE_DOCS` | no (off) | serve interactive docs at `/docs` (development only) |

Generate a key: `openssl rand -base64 48`. Secrets live only in the environment (or a git-ignored `.env`).

## Tests

```bash
python -m unittest discover -s tests -t .      # needs no extra packages (HTTP-layer tests are skipped)
pip install -r requirements-dev.txt && pytest  # everything, including the HTTP-layer tests
```

No test calls the real OpenAlex/Crossref/Semantic Scholar: sources are replaced by test doubles, so the suite is fast and repeatable.

## Structure

```text
ai-service/
├── app/
│   ├── main.py             FastAPI factory, middleware, error handlers
│   ├── pipeline.py         keywords -> search -> similarity -> gaps -> terms -> titles
│   ├── api/                routes, request schemas, API-key dependency
│   ├── core/               config, security, cache, errors, logging, serialization
│   ├── search/             providers (OpenAlex, Crossref, Semantic Scholar), parsing, search service
│   ├── nlp/                tokenising, RAKE-style keyword extraction, query building
│   ├── similarity/         TF-IDF, cosine, similarity indicator
│   ├── gap_analysis/       gap suggestions with evidence
│   ├── recommendations/    alternative titles, related terms
│   └── reports/            JSON report and Markdown renderer
├── tests/
└── docs/AI-SERVICE.md      methodology, thresholds, integration contract
```

## Limitations

- **No language model.** Keyword extraction, similarity and gap detection are deterministic text statistics (standard library only). This keeps results explainable and testable, but they are shallower than an LLM. `app/recommendations` and `app/gap_analysis` are the places to add one later, as long as retrieved facts stay separate from generated text.
- English-language text only; abstracts missing from a source are compared by title only (and flagged).
- Google Scholar is not queried (no official API); the backend or frontend may offer a manual search link instead.
- Results depend on what the three sources return for a handful of queries.
- The cache is per process; with several instances each has its own.
- Written without running `pip install`, FastAPI or the HTTP-layer tests in the authoring environment (no network there); the core logic tests were run (see docs).
