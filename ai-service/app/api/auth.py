"""Authentication endpoints for the AI service."""
from __future__ import annotations

from fastapi import APIRouter, Depends, Request

from app.api.deps import require_api_key
from app.core.config import Settings
from app.core.errors import ApiError, error_body


router = APIRouter(prefix="/api/auth", dependencies=[Depends(require_api_key)], tags=["auth"])


@router.get("/me")
async def me(request: Request) -> dict:
    """Return the authenticated user information."""
    settings: Settings = request.app.state.settings
    return {
        "user": {
            "id": "api-key-user",
            "name": "API Key User",
        },
        "scopes": ["research"],
    }