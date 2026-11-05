"""Shared request dependencies."""
from __future__ import annotations

from fastapi import Header, Request

from app.core.errors import ApiError
from app.core.security import is_valid_api_key
from app.pipeline import ResearchPipeline


async def require_api_key(request: Request, x_api_key: str | None = Header(default=None)) -> None:
    if not is_valid_api_key(x_api_key, request.app.state.settings.api_key):
        raise ApiError("UNAUTHORIZED", "A valid API key is required", 401)


def get_pipeline(request: Request) -> ResearchPipeline:
    return request.app.state.pipeline
