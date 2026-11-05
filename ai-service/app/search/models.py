"""Domain objects for literature search."""
from __future__ import annotations

from dataclasses import dataclass, field, replace
from enum import Enum


class ProviderStatus(str, Enum):
    OK = "ok"                      # the provider answered and returned papers
    NO_RESULTS = "no_results"      # the provider answered, but found nothing for our queries
    RATE_LIMITED = "rate_limited"  # the provider refused because of request limits
    UNAVAILABLE = "unavailable"    # timeout, network error or provider error: we do NOT know if papers exist


@dataclass(frozen=True)
class Paper:
    title: str
    sources: tuple[str, ...]
    abstract: str | None = None
    year: int | None = None
    authors: tuple[str, ...] = ()
    venue: str | None = None
    doi: str | None = None
    url: str | None = None
    citation_count: int | None = None

    @property
    def text(self) -> str:
        """Title plus abstract, as used for similarity and gap analysis."""
        return f"{self.title}. {self.abstract}" if self.abstract else self.title

    @property
    def has_abstract(self) -> bool:
        return bool(self.abstract and self.abstract.strip())

    def merged_with(self, other: "Paper") -> "Paper":
        """Combines two records of the same work found in different providers."""
        abstract = self.abstract
        if other.abstract and (not abstract or len(other.abstract) > len(abstract)):
            abstract = other.abstract
        citations = [c for c in (self.citation_count, other.citation_count) if c is not None]
        return replace(
            self,
            sources=tuple(dict.fromkeys(self.sources + other.sources)),
            abstract=abstract,
            year=self.year or other.year,
            authors=self.authors or other.authors,
            venue=self.venue or other.venue,
            doi=self.doi or other.doi,
            url=self.url or other.url,
            citation_count=max(citations) if citations else None,
        )


@dataclass(frozen=True)
class ProviderReport:
    provider: str
    status: ProviderStatus
    papers_found: int
    message: str = ""


@dataclass
class SearchOutcome:
    papers: list[Paper] = field(default_factory=list)
    providers: list[ProviderReport] = field(default_factory=list)
    queries: list[str] = field(default_factory=list)

    @property
    def all_providers_failed(self) -> bool:
        return bool(self.providers) and all(
            report.status in (ProviderStatus.UNAVAILABLE, ProviderStatus.RATE_LIMITED) and report.papers_found == 0
            for report in self.providers
        )
