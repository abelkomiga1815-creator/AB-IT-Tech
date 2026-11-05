package com.abittech.platform.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings, bound from {@code app.jwt.*} (environment: JWT_SECRET, JWT_EXPIRATION).
 * The application refuses to start if the secret is missing or shorter than 32 characters.
 *
 * @param secret       HMAC signing secret, at least 32 characters
 * @param expirationMs token lifetime in milliseconds
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "JWT_SECRET must be at least 32 characters long") String secret,
        @Positive long expirationMs) {
}
