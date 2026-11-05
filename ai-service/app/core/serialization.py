"""Turns domain objects into the JSON shape of the API: camelCase keys, like the Spring Boot backend."""
from __future__ import annotations

import dataclasses
from datetime import datetime, timezone
from enum import Enum
from typing import Any


def camel(name: str) -> str:
    head, *tail = name.split("_")
    return head + "".join(part.capitalize() for part in tail)


def to_api(value: Any) -> Any:
    if dataclasses.is_dataclass(value) and not isinstance(value, type):
        return {camel(f.name): to_api(getattr(value, f.name)) for f in dataclasses.fields(value)}
    if isinstance(value, Enum):
        return value.value
    if isinstance(value, datetime):
        return value.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")
    if isinstance(value, dict):
        return {str(key): to_api(item) for key, item in value.items()}
    if isinstance(value, (list, tuple, set, frozenset)):
        return [to_api(item) for item in value]
    return value
