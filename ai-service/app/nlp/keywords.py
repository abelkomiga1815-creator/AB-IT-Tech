"""Keyword and key-phrase extraction (RAKE-style) and search-query building.

Standard library only. Phrases are runs of content words separated by stop-words or punctuation;
a phrase scores higher when its words occur in long phrases, often, and in the title.
"""
from __future__ import annotations

import math
import re
from collections import Counter, defaultdict
from dataclasses import dataclass

from .text import SEGMENT_SPLIT, is_content_word, stem, tokenize

MAX_PHRASE_WORDS = 3
TITLE_BOOST = 1.5


@dataclass(frozen=True)
class Keyword:
    term: str
    score: float          # 0..1, relative to the best keyword of this text
    kind: str             # "phrase" (two or more words) or "term" (one word)
    in_title: bool

    @property
    def tokens(self) -> tuple[str, ...]:
        return tuple(self.term.split())


def _runs(text: str) -> list[list[tuple[str, str]]]:
    """Runs of (normalised, surface) content words, split at stop-words and punctuation."""
    runs: list[list[tuple[str, str]]] = []
    for segment in SEGMENT_SPLIT.split(text.lower()):
        current: list[tuple[str, str]] = []
        for token in tokenize(segment):
            if is_content_word(token):
                current.append((stem(token), token))
            elif current:
                runs.append(current)
                current = []
        if current:
            runs.append(current)
    return runs


def _chunks(run: list[tuple[str, str]]) -> list[list[tuple[str, str]]]:
    return [run[i:i + MAX_PHRASE_WORDS] for i in range(0, len(run), MAX_PHRASE_WORDS)]


def extract_keywords(title: str, abstract: str = "", max_keywords: int = 12) -> list[Keyword]:
    title = title or ""
    abstract = abstract or ""
    phrase_counts: Counter[tuple[str, ...]] = Counter()
    in_title: set[tuple[str, ...]] = set()
    surface_votes: dict[str, Counter[str]] = defaultdict(Counter)
    frequency: Counter[str] = Counter()
    degree: Counter[str] = Counter()

    for text, is_title in ((title, True), (abstract, False)):
        for run in _runs(text):
            for chunk in _chunks(run):
                key = tuple(norm for norm, _ in chunk)
                phrase_counts[key] += 1
                if is_title:
                    in_title.add(key)
                for norm, surface in chunk:
                    surface_votes[norm][surface] += 1
                    frequency[norm] += 1
                    degree[norm] += len(chunk)

    scored: list[tuple[float, tuple[str, ...]]] = []
    for phrase, count in phrase_counts.items():
        # RAKE word score (degree / frequency), weighted up for words that occur often
        score = sum(degree[word] / frequency[word] * (1 + math.log(frequency[word])) for word in phrase)
        score *= 1 + 0.25 * (count - 1)
        if phrase in in_title:
            score *= TITLE_BOOST
        scored.append((score, phrase))
    # best score first; ties keep first-seen order (Counter preserves insertion order)
    order = {phrase: index for index, phrase in enumerate(phrase_counts)}
    scored.sort(key=lambda item: (-item[0], order[item[1]]))

    selected: list[tuple[float, tuple[str, ...]]] = []
    for score, phrase in scored:
        if any(_is_subphrase(phrase, other) for _, other in selected):
            continue
        selected.append((score, phrase))
        if len(selected) >= max_keywords:
            break
    if not selected:
        return []

    best = selected[0][0]
    keywords = []
    for score, phrase in selected:
        words = [surface_votes[word].most_common(1)[0][0] for word in phrase]
        keywords.append(Keyword(
            term=" ".join(words),
            score=round(score / best, 4),
            kind="phrase" if len(phrase) > 1 else "term",
            in_title=phrase in in_title,
        ))
    return keywords


def _is_subphrase(small: tuple[str, ...], big: tuple[str, ...]) -> bool:
    if len(small) >= len(big):
        return False
    return any(big[i:i + len(small)] == small for i in range(len(big) - len(small) + 1))


def build_search_queries(keywords: list[Keyword], max_queries: int = 3, max_length: int = 150) -> list[str]:
    """Search queries made only of extracted key terms (never the student's full text)."""
    terms = [keyword.term for keyword in keywords]
    if not terms:
        return []
    candidates = [
        " ".join(terms[:3]),
        " ".join([terms[0], *terms[3:5]]) if len(terms) > 3 else "",
        terms[0],
    ]
    queries: list[str] = []
    for candidate in candidates:
        candidate = _dedupe_words(candidate)[:max_length].strip()
        if candidate and candidate not in queries:
            queries.append(candidate)
        if len(queries) >= max_queries:
            break
    return queries


def _dedupe_words(text: str) -> str:
    """Drops repeated words ("leaf images classify leaf images" -> "leaf images classify")."""
    seen: set[str] = set()
    words = []
    for word in text.split():
        if word not in seen:
            seen.add(word)
            words.append(word)
    return " ".join(words)
