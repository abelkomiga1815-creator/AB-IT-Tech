package com.abittech.platform.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the typed configuration properties.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, StorageProperties.class,
        AiProperties.class, RateLimitProperties.class})
public class PropertiesConfig {
}
