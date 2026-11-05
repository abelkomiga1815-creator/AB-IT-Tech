"""Service settings, read from environment variables (never hard-coded)."""
from __future__ import annotations

import os
from pathlib import Path
from dataclasses import dataclass
from typing import Mapping

MIN_API_KEY_LENGTH = 32


class ConfigError(Exception):
    """Raised at startup when the configuration is unusable."""


@dataclass(frozen=True)
class Settings:
    api_key: str
    contact_email: str = ""
    semantic_scholar_api_key: str = ""
    request_timeout_seconds: float = 10.0
    cache_ttl_seconds: int = 3600
    cache_max_entries: int = 500
    log_level: str = "INFO"
    enable_docs: bool = False
    openalex_base_url: str = "https://api.openalex.org"
    crossref_base_url: str = "https://api.crossref.org"
    semantic_scholar_base_url: str = "https://api.semanticscholar.org"

    @staticmethod
    def from_env(env: Mapping[str, str] | None = None) -> "Settings":
        if env is None:
          from dotenv import load_dotenv

          load_dotenv(Path(__file__).resolve().parents[2] / ".env")
          env = os.environ
        api_key = env.get("AI_SERVICE_API_KEY", "").strip()
        if len(api_key) < MIN_API_KEY_LENGTH:
            raise ConfigError(
                f"AI_SERVICE_API_KEY must be set and at least {MIN_API_KEY_LENGTH} characters long"
            )
        return Settings(
            api_key=api_key,
            contact_email=env.get("CONTACT_EMAIL", "").strip(),
            semantic_scholar_api_key=env.get("SEMANTIC_SCHOLAR_API_KEY", "").strip(),
            request_timeout_seconds=_positive_float(env, "PROVIDER_TIMEOUT_SECONDS", 10.0),
            cache_ttl_seconds=_positive_int(env, "CACHE_TTL_SECONDS", 3600),
            cache_max_entries=_positive_int(env, "CACHE_MAX_ENTRIES", 500),
            log_level=env.get("AI_SERVICE_LOG_LEVEL", "INFO").strip().upper() or "INFO",
            enable_docs=env.get("AI_SERVICE_ENABLE_DOCS", "").strip().lower() in ("1", "true", "yes"),
        )


def _positive_float(env: Mapping[str, str], name: str, default: float) -> float:
    raw = env.get(name, "").strip()
    if not raw:
        return default
    try:
        value = float(raw)
    except ValueError as exc:
        raise ConfigError(f"{name} must be a number") from exc
    if value <= 0:
        raise ConfigError(f"{name} must be greater than zero")
    return value


def _positive_int(env: Mapping[str, str], name: str, default: int) -> int:
    return int(_positive_float(env, name, float(default)))
