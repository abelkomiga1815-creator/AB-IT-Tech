"""Terms that recur in the related papers but are absent from the student's own text."""
from __future__ import annotations

from collections import Counter
from dataclasses import dataclass

from app.nlp.text import content_runs, content_tokens
from app.search.models import Paper

MAX_TERMS = 10
MIN_PAPERS = 3


@dataclass(frozen=True)
class RelatedTerm:
    term: str
    papers_mentioning: int


def related_terms(project_text: str, papers: list[Paper]) -> list[RelatedTerm]:
    if len(papers) < MIN_PAPERS:
        return []
    project_words = set(content_tokens(project_text))
    document_frequency: Counter[str] = Counter()
    for paper in papers:
        words = {w for w in content_tokens(paper.text) if len(w) > 3}
        pairs = {f"{a} {b}" for run in content_runs(paper.text) for a, b in zip(run, run[1:])
                 if len(a) > 3 and len(b) > 3}
        document_frequency.update(words | pairs)
    threshold = max(MIN_PAPERS, round(0.2 * len(papers)))
    candidates = []
    for term, count in document_frequency.items():
        if count < threshold:
            continue
        if any(word in project_words for word in term.split()):
            continue
        candidates.append(RelatedTerm(term, count))
    candidates.sort(key=lambda item: (-item.papers_mentioning, -len(item.term), item.term))
    return candidates[:MAX_TERMS]
