"""One error format for the whole service, matching the Spring Boot backend:
{"success": false, "code": "...", "message": "...", "timestamp": "..."}"""
from __future__ import annotations

from datetime import datetime, timezone


class ApiError(Exception):
    """An expected, client-facing error. The message must be safe to show to callers."""

    def __init__(self, code: str, message: str, status_code: int):
        super().__init__(message)
        self.code = code
        self.message = message
        self.status_code = status_code


def error_body(code: str, message: str, errors: list[dict[str, str]] | None = None) -> dict:
    body: dict = {
        "success": False,
        "code": code,
        "message": message,
        "timestamp": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    }
    if errors:
        body["errors"] = errors
    return body
