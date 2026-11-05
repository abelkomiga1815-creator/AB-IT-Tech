"""The three literature providers: OpenAlex, Crossref and Semantic Scholar."""
from __future__ import annotations

from typing import Protocol

from app.core.config import Settings

from .http import HttpClient, checked_json
from .models import Paper
from .parsing import parse_crossref, parse_openalex, parse_semantic_scholar

USER_AGENT = "AB-IT-TECH-ResearchEngine/0.1"


class Provider(Protocol):
    name: str

    async def search(self, query: str, limit: int) -> list[Paper]:
        """Returns papers, or raises ProviderUnavailable / ProviderRateLimited."""
        ...


class OpenAlexProvider:
    name = "openalex"

    def __init__(self, client: HttpClient, settings: Settings):
        self._client = client
        self._settings = settings

    async def search(self, query: str, limit: int) -> list[Paper]:
        params: dict[str, str | int] = {"search": query, "per-page": limit}
        if self._settings.contact_email:
            params["mailto"] = self._settings.contact_email
        result = await self._client.get_json(
            f"{self._settings.openalex_base_url}/works",
            params=params,
            headers={"User-Agent": USER_AGENT},
            timeout=self._settings.request_timeout_seconds,
        )
        return parse_openalex(checked_json(result, self.name))


class CrossrefProvider:
    name = "crossref"

    def __init__(self, client: HttpClient, settings: Settings):
        self._client = client
        self._settings = settings

    async def search(self, query: str, limit: int) -> list[Paper]:
        params: dict[str, str | int] = {
            "query.bibliographic": query,
            "rows": limit,
            "select": "DOI,title,abstract,issued,author,container-title,is-referenced-by-count,URL",
        }
        if self._settings.contact_email:
            params["mailto"] = self._settings.contact_email
        result = await self._client.get_json(
            f"{self._settings.crossref_base_url}/works",
            params=params,
            headers={"User-Agent": USER_AGENT},
            timeout=self._settings.request_timeout_seconds,
        )
        return parse_crossref(checked_json(result, self.name))


class SemanticScholarProvider:
    name = "semantic_scholar"

    def __init__(self, client: HttpClient, settings: Settings):
        self._client = client
        self._settings = settings

    async def search(self, query: str, limit: int) -> list[Paper]:
        headers = {"User-Agent": USER_AGENT}
        if self._settings.semantic_scholar_api_key:
            headers["x-api-key"] = self._settings.semantic_scholar_api_key
        result = await self._client.get_json(
            f"{self._settings.semantic_scholar_base_url}/graph/v1/paper/search",
            params={
                "query": query,
                "limit": limit,
                "fields": "title,abstract,year,authors,venue,externalIds,citationCount,url",
            },
            headers=headers,
            timeout=self._settings.request_timeout_seconds,
        )
        return parse_semantic_scholar(checked_json(result, self.name))


def default_providers(client: HttpClient, settings: Settings) -> list[Provider]:
    return [
        OpenAlexProvider(client, settings),
        CrossrefProvider(client, settings),
        SemanticScholarProvider(client, settings),
    ]
