package com.abittech.platform.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS for the browser frontends (GROUP 1 and GROUP 2). Origins come from configuration only;
 * when none are configured, no CORS headers are issued.
 */
@Configuration(proxyBeanMethods = false)
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (!properties.allowedOrigins().isEmpty()) {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(properties.allowedOrigins());
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
            configuration.setExposedHeaders(List.of("X-Request-Id", "Content-Disposition"));
            configuration.setAllowCredentials(false);
            configuration.setMaxAge(3600L);
            source.registerCorsConfiguration("/api/**", configuration);
        }
        return source;
    }
}
