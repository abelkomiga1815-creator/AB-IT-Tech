"""TF-IDF vectors and cosine similarity in plain Python."""
from __future__ import annotations

import math
from collections import Counter
from typing import Sequence

from app.nlp.text import content_runs, content_tokens

Vector = dict[str, float]


def features(text: str) -> list[str]:
    """Unigrams plus adjacent-word pairs, so shared phrases count more than shared single words."""
    pairs = [f"{a}_{b}" for run in content_runs(text) for a, b in zip(run, run[1:])]
    return content_tokens(text) + pairs


def build_vectors(documents: Sequence[Sequence[str]]) -> tuple[list[Vector], dict[str, float]]:
    """TF-IDF for every document (each a list of features). Returns (vectors, idf)."""
    count = len(documents)
    document_frequency: Counter[str] = Counter()
    for document in documents:
        document_frequency.update(set(document))
    idf = {term: math.log((1 + count) / (1 + df)) + 1.0 for term, df in document_frequency.items()}
    vectors: list[Vector] = []
    for document in documents:
        counts = Counter(document)
        vectors.append({term: (1.0 + math.log(n)) * idf[term] for term, n in counts.items()})
    return vectors, idf


def cosine(a: Vector, b: Vector) -> float:
    if not a or not b:
        return 0.0
    if len(a) > len(b):
        a, b = b, a
    dot = sum(weight * b.get(term, 0.0) for term, weight in a.items())
    norm = math.sqrt(sum(w * w for w in a.values())) * math.sqrt(sum(w * w for w in b.values()))
    return dot / norm if norm else 0.0


def similarity_scores(query_text: str, texts: Sequence[str]) -> list[float]:
    """Cosine similarity (0..1) of `query_text` to each text, with IDF computed over all of them."""
    documents = [features(query_text)] + [features(text) for text in texts]
    vectors, _ = build_vectors(documents)
    return [round(cosine(vectors[0], vector), 4) for vector in vectors[1:]]


def shared_terms(query_text: str, text: str, limit: int = 8) -> list[str]:
    """Words (not pairs) that appear in both texts, longest/rarest first as a rough relevance order."""
    query_words = set(content_tokens(query_text))
    common = [word for word in dict.fromkeys(content_tokens(text)) if word in query_words]
    common.sort(key=lambda word: (-len(word), word))
    return common[:limit]
