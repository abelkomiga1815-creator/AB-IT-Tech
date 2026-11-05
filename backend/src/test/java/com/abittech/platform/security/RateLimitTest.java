package com.abittech.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class RateLimitTest {

    /** A clock the test can move. */
    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-07T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static final Duration MINUTE = Duration.ofMinutes(1);

    @Test
    void allowsUpToTheLimitThenRefuses() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(new MutableClock());

        assertThat(limiter.tryAcquire("a", 3, MINUTE)).isTrue();
        assertThat(limiter.tryAcquire("a", 3, MINUTE)).isTrue();
        assertThat(limiter.tryAcquire("a", 3, MINUTE)).isTrue();
        assertThat(limiter.tryAcquire("a", 3, MINUTE)).isFalse();
        assertThat(limiter.tryAcquire("a", 3, MINUTE)).isFalse();
    }

    @Test
    void keysAreIndependent() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(new MutableClock());

        assertThat(limiter.tryAcquire("one", 1, MINUTE)).isTrue();
        assertThat(limiter.tryAcquire("one", 1, MINUTE)).isFalse();
        assertThat(limiter.tryAcquire("two", 1, MINUTE)).isTrue();
    }

    @Test
    void hitsExpireAfterTheWindow() {
        MutableClock clock = new MutableClock();
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(clock);
        assertThat(limiter.tryAcquire("a", 1, MINUTE)).isTrue();

        clock.advance(Duration.ofSeconds(59));
        assertThat(limiter.tryAcquire("a", 1, MINUTE)).isFalse();

        clock.advance(Duration.ofSeconds(2));
        assertThat(limiter.tryAcquire("a", 1, MINUTE)).isTrue();
    }

    @Test
    void refusedAttemptsDoNotExtendTheBlock() {
        MutableClock clock = new MutableClock();
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(clock);
        limiter.tryAcquire("a", 1, MINUTE);

        for (int i = 0; i < 5; i++) {
            clock.advance(Duration.ofSeconds(10));
            limiter.tryAcquire("a", 1, MINUTE);
        }
        clock.advance(Duration.ofSeconds(11));   // 61 s after the only accepted hit

        assertThat(limiter.tryAcquire("a", 1, MINUTE)).isTrue();
    }
}
