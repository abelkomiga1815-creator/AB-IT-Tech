"""A similarity *indicator*: how close the project is to the papers we found. Never a verdict."""
from __future__ import annotations

from dataclasses import dataclass
from enum import Enum

from app.search.models import Paper

from .tfidf import shared_terms, similarity_scores

HIGH_THRESHOLD = 0.50
MODERATE_THRESHOLD = 0.25
MAX_MATCHES = 5

DISCLAIMER = (
    "This is a similarity indicator based on the papers found in the sources searched. "
    "It is not a plagiarism check, and it does not prove or disprove that a project is original."
)


class SimilarityLevel(str, Enum):
    LOW = "low"
    MODERATE = "moderate"
    HIGH = "high"
    INSUFFICIENT_DATA = "insufficient_data"


@dataclass(frozen=True)
class ScoredPaper:
    paper: Paper
    score: float
    shared_terms: tuple[str, ...]
    basis: str  # "title_and_abstract" or "title_only"


@dataclass(frozen=True)
class SimilarityIndicator:
    level: SimilarityLevel
    max_score: float
    matches: tuple[ScoredPaper, ...]
    papers_compared: int
    papers_with_abstract: int
    explanation: str
    disclaimer: str = DISCLAIMER


def score_papers(project_text: str, papers: list[Paper]) -> list[ScoredPaper]:
    """Every paper scored against the project text, best match first."""
    scores = similarity_scores(project_text, [paper.text for paper in papers])
    scored = [
        ScoredPaper(
            paper=paper,
            score=score,
            shared_terms=tuple(shared_terms(project_text, paper.text)),
            basis="title_and_abstract" if paper.has_abstract else "title_only",
        )
        for paper, score in zip(papers, scores)
    ]
    scored.sort(key=lambda item: item.score, reverse=True)
    return scored


def compute_similarity(project_title: str, project_abstract: str | None, scored: list[ScoredPaper]) -> SimilarityIndicator:
    with_abstract = [item for item in scored if item.basis == "title_and_abstract"]
    if not scored:
        return SimilarityIndicator(
            level=SimilarityLevel.INSUFFICIENT_DATA, max_score=0.0, matches=(), papers_compared=0,
            papers_with_abstract=0,
            explanation="No papers could be retrieved from the sources, so no comparison was possible. "
                        "This says nothing about the originality of the project.",
        )
    if not with_abstract:
        return SimilarityIndicator(
            level=SimilarityLevel.INSUFFICIENT_DATA, max_score=scored[0].score, matches=tuple(scored[:MAX_MATCHES]),
            papers_compared=len(scored), papers_with_abstract=0,
            explanation="The papers found had no abstracts, so only titles were compared. "
                        "That is too little information for a meaningful indicator.",
        )

    best = with_abstract[0].score
    if best >= HIGH_THRESHOLD:
        level = SimilarityLevel.HIGH
        explanation = ("At least one paper is very close to the project text. Read it carefully and make sure your "
                       "contribution is clearly different, then discuss it with your supervisor.")
    elif best >= MODERATE_THRESHOLD:
        level = SimilarityLevel.MODERATE
        explanation = ("Some papers cover similar ground. This is normal for a research topic; use them as related "
                       "work and state clearly how your project differs.")
    else:
        level = SimilarityLevel.LOW
        explanation = ("No retrieved paper is close to the project text. This only reflects the papers that the "
                       "sources returned; other relevant work may exist that these searches did not find.")
    if len(with_abstract) < 5:
        explanation += f" Only {len(with_abstract)} paper(s) with abstracts were compared, so treat this with extra caution."
    return SimilarityIndicator(
        level=level,
        max_score=best,
        matches=tuple(scored[:MAX_MATCHES]),
        papers_compared=len(scored),
        papers_with_abstract=len(with_abstract),
        explanation=explanation,
    )
