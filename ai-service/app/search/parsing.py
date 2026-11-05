"""Converts provider JSON into Paper objects. Pure functions: easy to test, no network."""
from __future__ import annotations

import html
import re
from typing import Any

from .models import Paper

_TAGS = re.compile(r"<[^>]+>")
_SPACES = re.compile(r"\s+")
_DOI_PREFIX = re.compile(r"^(https?://)?(dx\.)?doi\.org/", re.IGNORECASE)
MAX_AUTHORS = 10


def normalize_doi(value: str | None) -> str | None:
    if not value:
        return None
    doi = _DOI_PREFIX.sub("", value.strip()).strip().lower()
    return doi or None


def _clean_text(value: str | None) -> str | None:
    if not value:
        return None
    text = _SPACES.sub(" ", html.unescape(_TAGS.sub(" ", value))).strip()
    # Crossref abstracts often start with the word "Abstract"
    text = re.sub(r"^abstract[:\s]+", "", text, flags=re.IGNORECASE)
    return text or None


def _as_int(value: Any) -> int | None:
    return value if isinstance(value, int) and not isinstance(value, bool) else None


def _doi_url(doi: str | None) -> str | None:
    return f"https://doi.org/{doi}" if doi else None


def reconstruct_abstract(inverted_index: dict[str, list[int]] | None) -> str | None:
    """OpenAlex ships abstracts as {word: [positions]}; put the words back in order."""
    if not inverted_index:
        return None
    positions: list[tuple[int, str]] = []
    for word, indexes in inverted_index.items():
        for index in indexes or []:
            positions.append((index, word))
    positions.sort()
    return " ".join(word for _, word in positions) or None


def parse_openalex(data: Any) -> list[Paper]:
    papers: list[Paper] = []
    for item in (data or {}).get("results", []) or []:
        title = _clean_text(item.get("display_name") or item.get("title"))
        if not title:
            continue
        doi = normalize_doi(item.get("doi"))
        authors = tuple(
            name for name in (
                (a.get("author") or {}).get("display_name") for a in (item.get("authorships") or [])[:MAX_AUTHORS]
            ) if name
        )
        venue = ((item.get("primary_location") or {}).get("source") or {}).get("display_name")
        papers.append(Paper(
            title=title,
            sources=("openalex",),
            abstract=_clean_text(reconstruct_abstract(item.get("abstract_inverted_index"))),
            year=_as_int(item.get("publication_year")),
            authors=authors,
            venue=venue,
            doi=doi,
            url=_doi_url(doi) or item.get("id"),
            citation_count=_as_int(item.get("cited_by_count")),
        ))
    return papers


def parse_crossref(data: Any) -> list[Paper]:
    papers: list[Paper] = []
    for item in ((data or {}).get("message") or {}).get("items", []) or []:
        titles = item.get("title") or []
        title = _clean_text(titles[0] if titles else None)
        if not title:
            continue
        doi = normalize_doi(item.get("DOI"))
        parts = ((item.get("issued") or {}).get("date-parts") or [[None]])[0]
        authors = tuple(
            " ".join(part for part in (a.get("given"), a.get("family")) if part)
            for a in (item.get("author") or [])[:MAX_AUTHORS]
            if a.get("family") or a.get("given")
        )
        containers = item.get("container-title") or []
        papers.append(Paper(
            title=title,
            sources=("crossref",),
            abstract=_clean_text(item.get("abstract")),
            year=_as_int(parts[0] if parts else None),
            authors=authors,
            venue=containers[0] if containers else None,
            doi=doi,
            url=_doi_url(doi) or item.get("URL"),
            citation_count=_as_int(item.get("is-referenced-by-count")),
        ))
    return papers


def parse_semantic_scholar(data: Any) -> list[Paper]:
    papers: list[Paper] = []
    for item in (data or {}).get("data", []) or []:
        title = _clean_text(item.get("title"))
        if not title:
            continue
        doi = normalize_doi((item.get("externalIds") or {}).get("DOI"))
        authors = tuple(
            a["name"] for a in (item.get("authors") or [])[:MAX_AUTHORS] if a.get("name")
        )
        papers.append(Paper(
            title=title,
            sources=("semantic_scholar",),
            abstract=_clean_text(item.get("abstract")),
            year=_as_int(item.get("year")),
            authors=authors,
            venue=item.get("venue") or None,
            doi=doi,
            url=item.get("url") or _doi_url(doi),
            citation_count=_as_int(item.get("citationCount")),
        ))
    return papers
