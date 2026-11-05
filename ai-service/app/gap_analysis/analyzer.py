"""Evidence-based research gap suggestions.

Every suggestion comes from counts and sentences found in the retrieved papers and carries that
evidence. Confidence is never "high": the analysis only sees what the sources returned.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from itertools import combinations
from statistics import median

from app.nlp.keywords import Keyword
from app.nlp.text import content_tokens, sentences
from app.search.models import Paper

MIN_PAPERS = 5
MAX_CONCEPTS = 6
MAX_EXAMPLES = 3
MAX_LIMITATIONS = 5
MAX_SENTENCE_LENGTH = 300

CAVEAT = (
    "These are suggestions based only on the papers retrieved from the sources searched. "
    "A combination that looks under-explored here may be covered by work these searches did not find. "
    "Check with your supervisor and search further before building your project on a gap."
)

_LIMITATION_PATTERNS = [re.compile(p, re.IGNORECASE) for p in (
    r"\bfuture (work|research|studies|investigation)",
    r"\bremains? (unexplored|unclear|unknown|open|an open)",
    r"\b(few|limited|scarce) (studies|research|work|evidence|attention)",
    r"\black of\b",
    r"\bhas not been (fully )?(studied|explored|investigated|addressed)",
    r"\bhave not been (fully )?(studied|explored|investigated|addressed)",
    r"\bfurther (research|work|investigation|study) (is|are) (needed|required)",
    r"\bone limitation\b|\bis limited by\b|\bunder-?explored\b|\bunder-?studied\b",
)]


@dataclass(frozen=True)
class PaperRef:
    title: str
    year: int | None
    url: str | None
    sources: tuple[str, ...]

    @staticmethod
    def of(paper: Paper) -> "PaperRef":
        return PaperRef(title=paper.title, year=paper.year, url=paper.url, sources=paper.sources)


@dataclass(frozen=True)
class Evidence:
    text: str
    papers: tuple[PaperRef, ...] = ()


@dataclass(frozen=True)
class GapSuggestion:
    type: str            # COMBINATION_UNDER_EXPLORED, CONCEPT_NOT_FOUND, LITTLE_RECENT_WORK, REPORTED_LIMITATIONS
    title: str
    description: str
    evidence: tuple[Evidence, ...]
    confidence: str      # "low" or "medium"; never "high"


@dataclass(frozen=True)
class ConceptCoverage:
    concept: str
    papers_mentioning: int


@dataclass(frozen=True)
class GapAnalysis:
    suggestions: tuple[GapSuggestion, ...]
    concepts: tuple[ConceptCoverage, ...]
    papers_analyzed: int
    papers_with_abstract: int
    caveat: str = CAVEAT
    note: str = ""


def _mentions(paper_tokens: set[str], concept_tokens: tuple[str, ...]) -> bool:
    return bool(concept_tokens) and all(token in paper_tokens for token in concept_tokens)


def analyze_gaps(keywords: list[Keyword], papers: list[Paper], current_year: int) -> GapAnalysis:
    with_abstract = sum(1 for paper in papers if paper.has_abstract)
    if len(papers) < MIN_PAPERS:
        return GapAnalysis(
            suggestions=(), concepts=(), papers_analyzed=len(papers), papers_with_abstract=with_abstract,
            note=f"Only {len(papers)} paper(s) were retrieved; at least {MIN_PAPERS} are needed to look for gaps reliably.",
        )

    concepts = [(k.term, tuple(content_tokens(k.term))) for k in keywords[:MAX_CONCEPTS]]
    concepts = [(term, tokens) for term, tokens in concepts if tokens]
    token_sets = [set(content_tokens(paper.text)) for paper in papers]
    covering: dict[str, list[int]] = {
        term: [i for i, tokens_of_paper in enumerate(token_sets) if _mentions(tokens_of_paper, tokens)]
        for term, tokens in concepts
    }
    coverage = tuple(ConceptCoverage(term, len(covering[term])) for term, _ in concepts)
    reliable = with_abstract >= len(papers) / 2

    suggestions: list[GapSuggestion] = []
    suggestions.extend(_combination_gaps(concepts, covering, papers, reliable))
    suggestions.extend(_missing_concepts(concepts, covering, papers))
    recency = _recency_gap(papers, current_year)
    if recency:
        suggestions.append(recency)
    limitations = _reported_limitations(papers)
    if limitations:
        suggestions.append(limitations)
    return GapAnalysis(
        suggestions=tuple(suggestions), concepts=coverage, papers_analyzed=len(papers),
        papers_with_abstract=with_abstract,
    )


def _refs(papers: list[Paper], indexes: list[int]) -> tuple[PaperRef, ...]:
    ranked = sorted(indexes, key=lambda i: papers[i].citation_count or 0, reverse=True)
    return tuple(PaperRef.of(papers[i]) for i in ranked[:MAX_EXAMPLES])


def _combination_gaps(concepts, covering, papers, reliable) -> list[GapSuggestion]:
    found: list[GapSuggestion] = []
    for (term_a, _), (term_b, _) in combinations(concepts, 2):
        side_a, side_b = covering[term_a], covering[term_b]
        both = set(side_a) & set(side_b)
        smaller = min(len(side_a), len(side_b))
        if smaller < 3 or len(both) > max(1, smaller // 10):
            continue
        confidence = "medium" if (smaller >= 8 and not both and reliable) else "low"
        evidence = (
            Evidence(f"{len(side_a)} of {len(papers)} retrieved papers mention \"{term_a}\".", _refs(papers, side_a)),
            Evidence(f"{len(side_b)} of {len(papers)} retrieved papers mention \"{term_b}\".", _refs(papers, side_b)),
            Evidence(f"{len(both)} retrieved paper(s) mention both."
                     + (" Example: " + papers[sorted(both)[0]].title if both else ""),
                     _refs(papers, sorted(both)) if both else ()),
        )
        found.append(GapSuggestion(
            type="COMBINATION_UNDER_EXPLORED",
            title=f"\"{term_a}\" together with \"{term_b}\"",
            description=(f"Each topic has its own literature in the retrieved papers, but they are rarely studied together. "
                         f"Combining them might be a direction worth investigating."),
            evidence=evidence,
            confidence=confidence,
        ))
    found.sort(key=lambda gap: gap.confidence != "medium")
    return found[:3]


def _missing_concepts(concepts, covering, papers) -> list[GapSuggestion]:
    found: list[GapSuggestion] = []
    for term, _ in concepts:
        if covering[term]:
            continue
        found.append(GapSuggestion(
            type="CONCEPT_NOT_FOUND",
            title=f"\"{term}\" does not appear in the retrieved papers",
            description=("None of the retrieved papers mention this term. Either the topic is niche, or the literature "
                         "uses different terminology. Try synonyms before concluding anything."),
            evidence=(Evidence(f"0 of {len(papers)} retrieved papers mention \"{term}\"."),),
            confidence="low",
        ))
    return found[:2]


def _recency_gap(papers: list[Paper], current_year: int) -> GapSuggestion | None:
    years = [paper.year for paper in papers if paper.year]
    if len(years) < 8:
        return None
    recent = [y for y in years if y >= current_year - 3]
    if median(years) > current_year - 8 or len(recent) / len(years) >= 0.10:
        return None
    return GapSuggestion(
        type="LITTLE_RECENT_WORK",
        title="Little recent work in the retrieved papers",
        description=("Most retrieved papers are several years old. Recent developments may deserve a fresh look, "
                     "or newer work may simply be missing from the search results."),
        evidence=(Evidence(f"Median publication year: {int(median(years))}; "
                           f"{len(recent)} of {len(years)} papers are from the last 3 years."),),
        confidence="low",
    )


def _reported_limitations(papers: list[Paper]) -> GapSuggestion | None:
    evidence: list[Evidence] = []
    for paper in papers:
        if not paper.abstract:
            continue
        for sentence in sentences(paper.abstract):
            if any(pattern.search(sentence) for pattern in _LIMITATION_PATTERNS):
                text = sentence if len(sentence) <= MAX_SENTENCE_LENGTH else sentence[:MAX_SENTENCE_LENGTH].rstrip() + "..."
                evidence.append(Evidence(text, (PaperRef.of(paper),)))
                break  # one sentence per paper
        if len(evidence) >= MAX_LIMITATIONS:
            break
    if not evidence:
        return None
    return GapSuggestion(
        type="REPORTED_LIMITATIONS",
        title="Limitations and future work reported by other authors",
        description=("These papers say, in their own words, what remains open. They can point to questions your "
                     "project could address. Read the full papers before relying on them."),
        evidence=tuple(evidence),
        confidence="low",
    )
