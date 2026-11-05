"""A small in-memory TTL + LRU cache for provider responses."""
from __future__ import annotations

import time
from collections import OrderedDict
from typing import Callable, Generic, Hashable, TypeVar

V = TypeVar("V")


class TTLCache(Generic[V]):
    """Entries expire after `ttl_seconds` and the least recently used entry is dropped when full."""

    def __init__(self, ttl_seconds: float, max_entries: int, clock: Callable[[], float] = time.monotonic):
        if ttl_seconds <= 0 or max_entries <= 0:
            raise ValueError("ttl_seconds and max_entries must be positive")
        self._ttl = ttl_seconds
        self._max = max_entries
        self._clock = clock
        self._items: "OrderedDict[Hashable, tuple[float, V]]" = OrderedDict()

    def get(self, key: Hashable) -> V | None:
        entry = self._items.get(key)
        if entry is None:
            return None
        stored_at, value = entry
        if self._clock() - stored_at >= self._ttl:
            del self._items[key]
            return None
        self._items.move_to_end(key)
        return value

    def set(self, key: Hashable, value: V) -> None:
        self._items[key] = (self._clock(), value)
        self._items.move_to_end(key)
        while len(self._items) > self._max:
            self._items.popitem(last=False)

    def __len__(self) -> int:
        return len(self._items)
