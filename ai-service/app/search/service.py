"""Runs all providers, handles their failures separately, caches answers and merges duplicates."""
from __future__ import annotations

import asyncio
import logging
import re
from typing import Sequence

from app.core.cache import TTLCache

from .http import ProviderRateLimited, ProviderUnavailable
from .models import Paper, ProviderReport, ProviderStatus, SearchOutcome
from .providers import Provider

log = logging.getLogger(__name__)
_NON_ALNUM = re.compile(r"[^a-z0-9]+")


def _title_key(title: str) -> str:
    return _NON_ALNUM.sub("", title.lower())


def merge_papers(papers: Sequence[Paper]) -> list[Paper]:
    """One record per work: same DOI, or same normalised title, means the same paper."""
    by_doi: dict[str, int] = {}
    by_title: dict[str, int] = {}
    merged: list[Paper] = []
    for paper in papers:
        title_key = _title_key(paper.title)
        index = by_doi.get(paper.doi) if paper.doi else None
        if index is None and title_key:
            index = by_title.get(title_key)
        if index is None:
            merged.append(paper)
            index = len(merged) - 1
        else:
            merged[index] = merged[index].merged_with(paper)
        current = merged[index]
        if current.doi:
            by_doi[current.doi] = index
        if title_key:
            by_title[title_key] = index
    return merged


class SearchService:
    def __init__(self, providers: Sequence[Provider], cache: TTLCache[list[Paper]], provider_timeout: float = 10.0):
        self._providers = list(providers)
        self._cache = cache
        # hard ceiling per provider call, on top of the HTTP client's own timeout
        self._call_timeout = provider_timeout + 2.0

    async def search(self, queries: Sequence[str], limit_per_provider: int) -> SearchOutcome:
        outcome = SearchOutcome(queries=list(queries))
        if not queries or not self._providers:
            return outcome
        results = await asyncio.gather(*(self._run_provider(p, queries, limit_per_provider) for p in self._providers))
        collected: list[Paper] = []
        for papers, report in results:
            collected.extend(papers)
            outcome.providers.append(report)
        outcome.papers = merge_papers(collected)
        return outcome

    async def _run_provider(
        self, provider: Provider, queries: Sequence[str], limit: int
    ) -> tuple[list[Paper], ProviderReport]:
        papers: list[Paper] = []
        failure: ProviderStatus | None = None
        message = ""
        for query in queries:  # one query after the other: polite to the provider's rate limits
            key = (provider.name, query, limit)
            cached = self._cache.get(key)
            if cached is not None:
                papers.extend(cached)
                continue
            try:
                found = await asyncio.wait_for(provider.search(query, limit), timeout=self._call_timeout)
            except ProviderRateLimited:
                failure, message = ProviderStatus.RATE_LIMITED, "The provider asked us to slow down."
                break
            except (ProviderUnavailable, asyncio.TimeoutError) as exc:
                failure = ProviderStatus.UNAVAILABLE
                message = "The provider did not answer in time." if isinstance(exc, asyncio.TimeoutError) \
                    else "The provider could not be reached or returned an error."
                break
            except Exception:  # a bug in one provider must not take the whole analysis down
                log.exception("Unexpected error in provider %s", provider.name)
                failure, message = ProviderStatus.UNAVAILABLE, "Unexpected error while querying the provider."
                break
            self._cache.set(key, found)
            papers.extend(found)

        papers = merge_papers(papers)
        if failure is not None:
            suffix = f" {len(papers)} paper(s) were obtained before the problem." if papers else ""
            return papers, ProviderReport(provider.name, failure, len(papers), message + suffix)
        if not papers:
            return papers, ProviderReport(provider.name, ProviderStatus.NO_RESULTS, 0,
                                          "The provider answered but found no matching papers.")
        return papers, ProviderReport(provider.name, ProviderStatus.OK, len(papers))
