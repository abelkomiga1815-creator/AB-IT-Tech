"""Shared test doubles and sample data. No network, no real providers."""
from __future__ import annotations

from app.search.http import HttpResult, ProviderRateLimited, ProviderUnavailable
from app.search.models import Paper, ProviderReport, ProviderStatus, SearchOutcome

PROJECT_TITLE = "Crop disease detection on smartphones"
PROJECT_ABSTRACT = "A mobile app that detects crop disease from leaf images using deep learning."


def paper(title: str, abstract: str | None = "", year: int | None = 2022, doi: str | None = None,
          sources: tuple[str, ...] = ("openalex",), citations: int | None = None) -> Paper:
    return Paper(title=title, sources=sources, abstract=abstract or None, year=year, doi=doi,
                 url=f"https://doi.org/{doi}" if doi else None, citation_count=citations)


def crop_papers(count: int = 8) -> list[Paper]:
    """Papers about crop disease detection (topic A), none about smartphones."""
    return [
        paper(f"Crop disease detection with deep learning {i}",
              "We detect crop disease from leaf images using convolutional networks. "
              "Future work should address field conditions.",
              year=2018 + i % 3, doi=f"10.1/crop{i}", citations=i)
        for i in range(count)
    ]


def mobile_papers(count: int = 5) -> list[Paper]:
    """Papers about smartphone deployment (topic B), none about crop disease."""
    return [
        paper(f"Smartphone deployment of mobile applications {i}",
              "Mobile smartphone applications for rural users deployed on low-cost devices.",
              year=2021, doi=f"10.2/mobile{i}", sources=("crossref",))
        for i in range(count)
    ]


class FakeProvider:
    """A provider that returns canned papers, or raises the given exception."""

    def __init__(self, name: str, papers: list[Paper] | None = None, error: Exception | None = None):
        self.name = name
        self._papers = papers or []
        self._error = error
        self.calls: list[tuple[str, int]] = []

    async def search(self, query: str, limit: int) -> list[Paper]:
        self.calls.append((query, limit))
        if self._error:
            raise self._error
        return list(self._papers)


class FakeHttpClient:
    """Records requests and answers with a fixed result (or raises)."""

    def __init__(self, result: HttpResult | None = None, error: Exception | None = None):
        self.result = result
        self.error = error
        self.requests: list[dict] = []

    async def get_json(self, url, *, params, headers, timeout):
        self.requests.append({"url": url, "params": dict(params), "headers": dict(headers), "timeout": timeout})
        if self.error:
            raise self.error
        return self.result

    async def aclose(self) -> None:
        return None


class FakeSearchService:
    """Stands in for SearchService in pipeline/API tests."""

    def __init__(self, papers: list[Paper], providers: list[ProviderReport] | None = None):
        self.papers = papers
        self.providers = providers or [ProviderReport("openalex", ProviderStatus.OK, len(papers))]
        self.calls = 0
        self.last_limit = None
        self.error: Exception | None = None

    async def search(self, queries, limit):
        self.calls += 1
        self.last_limit = limit
        if self.error:
            raise self.error
        return SearchOutcome(papers=list(self.papers), providers=list(self.providers), queries=list(queries))


RATE_LIMITED = ProviderRateLimited("test")
UNAVAILABLE = ProviderUnavailable("test")
