package com.abittech.platform.security;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window limiter: at most {@code limit} hits per key within {@code window}.
 * State is per application instance (a restart or a second instance starts with empty counters).
 */
public class SlidingWindowRateLimiter {

    private static final int EVICTION_THRESHOLD = 10_000;

    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** @return {@code true} and records the hit if the key is under its limit, {@code false} otherwise */
    public boolean tryAcquire(String key, int limit, Duration window) {
        long now = clock.millis();
        long cutoff = now - window.toMillis();
        if (hits.size() > EVICTION_THRESHOLD) {
            hits.entrySet().removeIf(entry -> isIdle(entry.getValue(), cutoff));
        }
        Deque<Long> timestamps = hits.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private static boolean isIdle(Deque<Long> timestamps, long cutoff) {
        synchronized (timestamps) {
            return timestamps.isEmpty() || timestamps.peekLast() <= cutoff;
        }
    }

    int trackedKeys() {
        return hits.size();
    }
}
