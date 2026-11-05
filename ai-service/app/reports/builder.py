"""Builds the structured research report. Retrieved facts and the tool's own analysis are kept apart."""
from __future__ import annotations

from typing import Any

from app.core.serialization import to_api
from app.pipeline import AnalysisResult
from app.search.models import ProviderStatus

MAX_RELATED_WORKS = 15

METHODOLOGY = [
    "Key terms were extracted from the project title and abstract with a statistical phrase-scoring method.",
    "Only those key terms (never the full text) were sent as search queries to OpenAlex, Crossref and Semantic Scholar.",
    "The returned papers were de-duplicated and compared with the project text using TF-IDF cosine similarity.",
    "Gap suggestions come from counting which key concepts appear together in the retrieved papers, from "
    "publication years, and from limitation sentences in the abstracts.",
    "Alternative titles are templates filled with your own key terms. No language model was used.",
]

_STATUS_TEXT = {
    ProviderStatus.OK: "answered",
    ProviderStatus.NO_RESULTS: "answered, no matching papers",
    ProviderStatus.RATE_LIMITED: "rate limited (results may be incomplete)",
    ProviderStatus.UNAVAILABLE: "unavailable (results may be incomplete)",
}


def build_report(result: AnalysisResult) -> dict[str, Any]:
    outcome = result.outcome
    sources = to_api(outcome.providers)
    related = [
        {
            **to_api(item.paper),
            "similarityScore": item.score,
            "sharedTerms": list(item.shared_terms),
            "basis": item.basis,
        }
        for item in result.ranked_papers[:MAX_RELATED_WORKS]
    ]
    return {
        "generatedAt": to_api(result.generated_at),
        "project": {"title": result.title, "hasAbstract": bool(result.abstract)},
        "retrieved": {
            "queries": result.queries,
            "sources": sources,
            "papersFound": len(outcome.papers),
            "relatedWork": related,
        },
        "analysis": {
            "keywords": to_api(result.keywords),
            "similarity": to_api(result.similarity),
            "gapAnalysis": to_api(result.gaps),
            "relatedTerms": to_api(result.related_terms),
            "alternativeTitles": to_api(result.titles),
        },
        "methodology": METHODOLOGY,
        "notices": _notices(result),
    }


def _notices(result: AnalysisResult) -> list[str]:
    notices = [
        result.similarity.disclaimer,
        "Only papers returned by the sources searched were considered. Other relevant work may exist.",
    ]
    problems = [
        f"{report.provider} ({_STATUS_TEXT[report.status]})"
        for report in result.outcome.providers
        if report.status in (ProviderStatus.RATE_LIMITED, ProviderStatus.UNAVAILABLE)
    ]
    if problems:
        notices.append("Some sources had problems: " + ", ".join(problems) + ". Run the analysis again later for fuller results.")
    if result.outcome.papers and result.similarity.papers_with_abstract < len(result.outcome.papers):
        notices.append("Some papers had no abstract; those were compared by title only.")
    return notices
