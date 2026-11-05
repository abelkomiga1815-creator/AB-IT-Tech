"""Alternative title ideas. Template-based: built from the student's own key terms and detected gaps,
so they are starting points to discuss, not finished titles."""
from __future__ import annotations

from dataclasses import dataclass

from app.gap_analysis.analyzer import GapAnalysis
from app.nlp.keywords import Keyword

_SMALL_WORDS = {"a", "an", "and", "as", "at", "by", "for", "in", "of", "on", "or", "the", "to", "with"}
MAX_TITLES = 5


@dataclass(frozen=True)
class TitleSuggestion:
    title: str
    rationale: str


def title_case(text: str) -> str:
    words = text.split()
    result = []
    for index, word in enumerate(words):
        lower = word.lower()
        result.append(lower if (index > 0 and lower in _SMALL_WORDS) else word[:1].upper() + word[1:])
    return " ".join(result)


def suggest_titles(project_title: str, keywords: list[Keyword], gaps: GapAnalysis | None) -> list[TitleSuggestion]:
    terms = [title_case(keyword.term) for keyword in keywords[:4]]
    if not terms:
        return []
    main = terms[0]
    other = terms[1] if len(terms) > 1 else None

    candidates: list[TitleSuggestion] = []
    if gaps:
        for gap in gaps.suggestions:
            if gap.type == "COMBINATION_UNDER_EXPLORED" and gap.title.count('"') >= 4:
                parts = gap.title.split('"')
                candidates.append(TitleSuggestion(
                    f"Exploring {title_case(parts[1])} with {title_case(parts[3])}: An Under-Studied Combination",
                    "Based on a combination that appears rarely in the retrieved papers.",
                ))
                break
    if other:
        candidates += [
            TitleSuggestion(f"{main} in the Context of {other}", "Frames your main topic within a second concept."),
            TitleSuggestion(f"A Comparative Study of Approaches to {main} for {other}",
                            "Turns the topic into a comparison of existing approaches."),
            TitleSuggestion(f"Design and Evaluation of a {main} Solution for {other}",
                            "Emphasises building and evaluating something concrete."),
            TitleSuggestion(f"Towards Improved {main}: Evidence from {other}",
                            "Emphasises evidence and improvement over existing practice."),
        ]
    candidates += [
        TitleSuggestion(f"{main}: Challenges, Limitations and Opportunities",
                        "A broader, review-style framing of your main topic."),
        TitleSuggestion(f"An Empirical Evaluation of {main}", "Emphasises measured results."),
    ]

    original = project_title.strip().lower()
    seen: set[str] = set()
    unique: list[TitleSuggestion] = []
    for candidate in candidates:
        key = candidate.title.lower()
        if key == original or key in seen:
            continue
        seen.add(key)
        unique.append(candidate)
    return unique[:MAX_TITLES]
