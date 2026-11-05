package com.abittech.platform.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS settings, bound from {@code app.cors.*} (environment: CORS_ALLOWED_ORIGINS, comma-separated).
 * Empty means no cross-origin browser access is allowed.
 *
 * @param allowedOrigins origins (for example {@code http://localhost:3000}) allowed to call the API
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
