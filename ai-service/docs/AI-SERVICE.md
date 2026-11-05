# AI Research & Innovation Engine: methodology and integration

## 1. How an analysis works

```text
title + abstract
   -> keyword extraction (RAKE-style)            app/nlp/keywords.py
   -> 1-3 search queries made of key terms only  build_search_queries
   -> OpenAlex, Crossref, Semantic Scholar        app/search/*   (concurrent, cached, failures isolated)
   -> merge duplicates (same DOI or normalised title)
   -> TF-IDF cosine similarity to the project text, ranking        app/similarity/*
   -> similarity indicator, gap suggestions, related terms, title ideas
   -> report (JSON / Markdown)
```

### Keyword extraction
Text is split at punctuation and stop-words; the remaining runs of words (up to 3 words) are candidate phrases. A phrase scores the sum of its words' RAKE score (degree / frequency, weighted by how often the word occurs), multiplied for repeated phrases and boosted 1.5x if it appears in the title. Sub-phrases of selected phrases are dropped.

### Search
Only the extracted terms are sent. Each source is called with one query after another (polite to rate limits); sources run concurrently. A call has the HTTP timeout plus a hard ceiling. Successful answers are cached (TTL); failures are never cached. Sources:

- OpenAlex `GET /works?search=...` (abstracts are rebuilt from the inverted index)
- Crossref `GET /works?query.bibliographic=...` (markup removed from abstracts)
- Semantic Scholar `GET /graph/v1/paper/search?query=...`

### Source statuses
`ok`, `no_results` (answered, found nothing), `rate_limited`, `unavailable` (timeout, network error, unusable answer). If a source fails after delivering some papers, the papers are kept and the status still names the failure.

### Similarity indicator
TF-IDF over unigrams and adjacent word pairs, cosine similarity between the project text and each paper's title + abstract. The level comes from the best score among papers that have abstracts:

| Best score | Level |
|------------|-------|
| >= 0.50 | `high` |
| >= 0.25 | `moderate` |
| below | `low` |
| no papers, or only titles | `insufficient_data` |

The thresholds are heuristics for short texts, not calibrated truths (`app/similarity/indicator.py`). Fewer than 5 papers with abstracts adds an explicit caution. The result always carries the disclaimer "not a plagiarism check".

### Gap suggestions (all need at least 5 retrieved papers)
1. **Combination under-explored**: two key concepts each appear in at least 3 papers, but together in at most 10% of the smaller group. `medium` confidence only with at least 8 papers on the smaller side, no overlap, and abstracts for at least half of the papers; otherwise `low`.
2. **Concept not found**: a key concept appears in none of the papers (maybe niche, maybe different terminology).
3. **Little recent work**: median year is 8+ years old and under 10% of papers are from the last 3 years.
4. **Reported limitations**: up to 5 sentences from abstracts that say things like "future work", "remains unexplored", "few studies", with the paper and link.

Every suggestion lists its evidence. Confidence is never `high`.

### Related terms and titles
Related terms: words and word pairs found in at least 20% of the papers (minimum 3) but not in the student's text. Titles: templates filled with the student's own key terms (and a combination title when a combination gap exists); each has a rationale. They are starting points for a conversation with a supervisor.

## 2. API contract

Authentication: header `X-API-Key: <AI_SERVICE_API_KEY>` on every `/api/v1/**` request (401 `UNAUTHORIZED` otherwise). Errors: 400 `VALIDATION_ERROR` (with `errors[]`), 401, 404 `RESOURCE_NOT_FOUND`, 405 `METHOD_NOT_ALLOWED`, 500 `INTERNAL_ERROR`. Every response has `X-Request-Id`.

`POST /api/v1/research/report` -> `data`:

```json
{
  "generatedAt": "2026-10-07T12:00:00Z",
  "project": {"title": "...", "hasAbstract": true},
  "retrieved": {
    "queries": ["..."],
    "sources": [{"provider": "openalex", "status": "ok", "papersFound": 13, "message": ""}],
    "papersFound": 13,
    "relatedWork": [{"title": "...", "authors": ["..."], "year": 2021, "venue": "...", "doi": "...", "url": "https://doi.org/...",
                     "sources": ["openalex"], "citationCount": 4, "abstract": "...",
                     "similarityScore": 0.41, "sharedTerms": ["crop"], "basis": "title_and_abstract"}]
  },
  "analysis": {
    "keywords": [{"term": "crop disease detection", "score": 1.0, "kind": "phrase", "inTitle": true}],
    "similarity": {"level": "moderate", "maxScore": 0.44, "matches": [], "papersCompared": 13,
                   "papersWithAbstract": 13, "explanation": "...", "disclaimer": "..."},
    "gapAnalysis": {"suggestions": [{"type": "COMBINATION_UNDER_EXPLORED", "title": "...", "description": "...",
                                     "evidence": [{"text": "...", "papers": []}], "confidence": "low"}],
                    "concepts": [], "papersAnalyzed": 13, "papersWithAbstract": 13, "caveat": "...", "note": ""},
    "relatedTerms": [{"term": "convolutional", "papersMentioning": 8}],
    "alternativeTitles": [{"title": "...", "rationale": "..."}]
  },
  "methodology": ["..."],
  "notices": ["..."]
}
```

Timing: an analysis performs up to 3 queries per source, so allow roughly 5-30 seconds when sources are slow; callers should use a client timeout above the service's `PROVIDER_TIMEOUT_SECONDS x 3`.

## 3. Integration with the Spring Boot backend (planned for the integration stage)

- The backend calls this service over HTTP with the shared key; browsers never reach it. Calls come from a new client in the backend's `ai` package, triggered by an authenticated student for one of their own projects.
- The backend stores results: `research_analyses` (project, report JSON, timestamp) and `research_sources` (papers saved for a project). This service stays stateless.
- Provider statuses and the disclaimer must be shown to the student with the results.
- Run it on an internal network only; set the same `AI_SERVICE_API_KEY` on both sides.

## 4. Verification status

Run in the authoring environment: 91 unit tests for keyword extraction, similarity, gap analysis, titles, provider request building and parsing, search service (failures, caching, merging, timeouts), configuration, security, serialization, pipeline and report (`python -m unittest discover -s tests -t .`).

Not run there: the FastAPI layer (`app/main.py`, `app/api/*`, `tests/test_api.py`) and the real HTTP client, because fastapi/httpx could not be installed and there was no network. No real call to OpenAlex, Crossref or Semantic Scholar has been made, so response-format assumptions (documented in `app/search/parsing.py`) still need a first live check.
