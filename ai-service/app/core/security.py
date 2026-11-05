"""Service-to-service authentication: a shared secret in the X-API-Key header."""
from __future__ import annotations

import hmac


def is_valid_api_key(provided: str | None, expected: str) -> bool:
    """Constant-time comparison, so response timing does not reveal the key."""
    if not provided or not expected:
        return False
    return hmac.compare_digest(provided.encode("utf-8"), expected.encode("utf-8"))
