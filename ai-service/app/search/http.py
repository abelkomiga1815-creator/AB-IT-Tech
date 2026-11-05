"""A tiny HTTP abstraction, so providers can be tested without network access."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping, Protocol


class ProviderUnavailable(Exception):
    """Timeout, network failure, or an unusable answer from the provider."""


class ProviderRateLimited(Exception):
    """The provider answered 429 (too many requests)."""


@dataclass(frozen=True)
class HttpResult:
    status: int
    data: Any | None


class HttpClient(Protocol):
    async def get_json(
        self, url: str, *, params: Mapping[str, str | int], headers: Mapping[str, str], timeout: float
    ) -> HttpResult:
        ...

    async def aclose(self) -> None:
        ...


class HttpxClient:
    """Production HTTP client based on httpx."""

    def __init__(self) -> None:
        import httpx  # imported here so the rest of the package works without httpx installed

        self._httpx = httpx
        self._client = httpx.AsyncClient(follow_redirects=True)

    async def get_json(
        self, url: str, *, params: Mapping[str, str | int], headers: Mapping[str, str], timeout: float
    ) -> HttpResult:
        try:
            response = await self._client.get(url, params=dict(params), headers=dict(headers), timeout=timeout)
        except self._httpx.TimeoutException as exc:
            raise ProviderUnavailable("timeout") from exc
        except self._httpx.HTTPError as exc:
            raise ProviderUnavailable("network error") from exc
        try:
            data = response.json()
        except ValueError:
            data = None
        return HttpResult(status=response.status_code, data=data)

    async def aclose(self) -> None:
        await self._client.aclose()


def checked_json(result: HttpResult, provider: str) -> Any:
    """Turns HTTP-level problems into the two provider exceptions; returns the JSON body otherwise."""
    if result.status == 429:
        raise ProviderRateLimited(provider)
    if result.status != 200 or result.data is None:
        raise ProviderUnavailable(f"{provider} answered with status {result.status}")
    return result.data
