"""The research pipeline: keywords -> search -> similarity -> gaps -> related terms -> titles."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Callable

from app.gap_analysis.analyzer import GapAnalysis, analyze_gaps
from app.nlp.keywords import Keyword, build_search_queries, extract_keywords
from app.recommendations.terms import RelatedTerm, related_terms
from app.recommendations.titles import TitleSuggestion, suggest_titles
from app.search.models import SearchOutcome
from app.search.service import SearchService
from app.similarity.indicator import ScoredPaper, SimilarityIndicator, compute_similarity, score_papers


@dataclass(frozen=True)
class AnalysisResult:
    title: str
    abstract: str | None
    keywords: list[Keyword]
    queries: list[str]
    outcome: SearchOutcome
    ranked_papers: list[ScoredPaper]
    similarity: SimilarityIndicator
    gaps: GapAnalysis
    related_terms: list[RelatedTerm]
    titles: list[TitleSuggestion]
    generated_at: datetime


class ResearchPipeline:
    def __init__(self, search_service: SearchService, clock: Callable[[], datetime] | None = None):
        self._search = search_service
        self._clock = clock or (lambda: datetime.now(timezone.utc))

    @staticmethod
    def extract(title: str, abstract: str | None) -> tuple[list[Keyword], list[str]]:
        keywords = extract_keywords(title, abstract or "")
        return keywords, build_search_queries(keywords)

    async def analyze(self, title: str, abstract: str | None, max_results: int) -> AnalysisResult:
        keywords, queries = self.extract(title, abstract)
        project_text = f"{title}. {abstract}" if abstract else title
        outcome = await self._search.search(queries, max_results)
        ranked = score_papers(project_text, outcome.papers)
        now = self._clock()
        gaps = analyze_gaps(keywords, [item.paper for item in ranked], current_year=now.year)
        return AnalysisResult(
            title=title,
            abstract=abstract,
            keywords=keywords,
            queries=queries,
            outcome=outcome,
            ranked_papers=ranked,
            similarity=compute_similarity(title, abstract, ranked),
            gaps=gaps,
            related_terms=related_terms(project_text, outcome.papers),
            titles=suggest_titles(title, keywords, gaps),
            generated_at=now,
        )
