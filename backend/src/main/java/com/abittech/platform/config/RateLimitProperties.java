package com.abittech.platform.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Per-client request limits for public endpoints, bound from {@code app.rate-limit.*}.
 *
 * @param authPerMinute    login and registration attempts per client address per minute
 * @param inquiryPerMinute guest inquiries per client address per minute
 */
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(boolean enabled, @Positive int authPerMinute, @Positive int inquiryPerMinute) {
}
