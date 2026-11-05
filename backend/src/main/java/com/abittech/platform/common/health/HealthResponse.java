package com.abittech.platform.common.health;

/**
 * Response body of {@code GET /api/v1/health}.
 */
public record HealthResponse(boolean success, String status) {

    public static HealthResponse up() {
        return new HealthResponse(true, "UP");
    }
}
