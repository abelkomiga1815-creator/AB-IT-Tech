package com.abittech.platform.ai;

import java.net.http.HttpClient;
import java.time.Duration;

import com.abittech.platform.config.AiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AiConfig {

    @Bean
    public AiClient aiClient(AiProperties properties) {
        return new AiClient(properties);
    }
}
