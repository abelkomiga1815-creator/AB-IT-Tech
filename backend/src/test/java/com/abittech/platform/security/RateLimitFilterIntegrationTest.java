package com.abittech.platform.security;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;

/**
 * Own application context with tiny limits (the shared test configuration allows thousands, because
 * many tests register and log in). The two tests use different buckets, so they do not depend on order.
 */
@TestPropertySource(properties = {
        "app.rate-limit.auth-per-minute=3",
        "app.rate-limit.inquiry-per-minute=2"})
class RateLimitFilterIntegrationTest extends IntegrationTestBase {

    @Test
    void repeatedLoginAttemptsAreStoppedWithA429() throws Exception {
        Map<String, String> wrong = Map.of("email", "nobody-" + UUID.randomUUID() + "@example.com", "password", "Wrong-Passw0rd");

        for (int attempt = 0; attempt < 3; attempt++) {
            send(HttpMethod.POST, "/api/v1/auth/login", null, wrong)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        send(HttpMethod.POST, "/api/v1/auth/login", null, wrong)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        // other endpoints, and other methods on the same path family, are not affected
        send(HttpMethod.GET, "/api/v1/health", null, null).andExpect(status().isOk());
        send(HttpMethod.GET, "/api/v1/services", null, null).andExpect(status().isOk());
    }

    @Test
    void guestInquiriesAreLimitedSeparately() throws Exception {
        Map<String, Object> inquiry = Map.of("name", "Spammer", "email", "spam@example.com",
                "message", "Buy my product now, please.");

        send(HttpMethod.POST, "/api/v1/inquiries", null, inquiry).andExpect(status().isCreated());
        send(HttpMethod.POST, "/api/v1/inquiries", null, inquiry).andExpect(status().isCreated());
        send(HttpMethod.POST, "/api/v1/inquiries", null, inquiry)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }
}
