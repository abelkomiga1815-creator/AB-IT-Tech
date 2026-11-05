package com.abittech.platform.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a single UTC {@link Clock}. Components that need "now" should inject it
 * instead of calling {@code Instant.now()}, which keeps them deterministic in tests.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
