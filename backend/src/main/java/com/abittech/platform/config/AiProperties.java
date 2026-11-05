package com.abittech.platform.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Connection to the Python AI research engine (GROUP 4), bound from {@code app.ai.*}
 * (environment: AI_SERVICE_URL, AI_SERVICE_API_KEY).
 *
 * @param apiKey                   shared secret sent as {@code X-API-Key}; must match the AI service's key
 * @param readTimeoutMs            an analysis queries three external sources, so this is generous
 * @param minSecondsBetweenAnalyses minimum gap between two analyses of the same project
 */
@Validated
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        @NotBlank String baseUrl,
        @NotBlank @Size(min = 32, message = "AI_SERVICE_API_KEY must be at least 32 characters long") String apiKey,
        @Positive int connectTimeoutMs,
        @Positive int readTimeoutMs,
        @PositiveOrZero int minSecondsBetweenAnalyses) {
}
