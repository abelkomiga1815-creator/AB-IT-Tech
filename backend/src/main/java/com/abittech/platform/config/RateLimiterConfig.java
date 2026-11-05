package com.abittech.platform.config;

import java.time.Clock;

import com.abittech.platform.security.SlidingWindowRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RateLimiterConfig {

    @Bean
    public SlidingWindowRateLimiter slidingWindowRateLimiter(Clock clock) {
        return new SlidingWindowRateLimiter(clock);
    }
}
