package com.abittech.platform.common.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight liveness check for clients (GROUP 1, GROUP 2, GROUP 4) and load balancers.
 * <p>
 * It only confirms that the API process answers HTTP requests. Database connectivity is
 * reported by Spring Boot Actuator at {@code /actuator/health}.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.up();
    }
}
