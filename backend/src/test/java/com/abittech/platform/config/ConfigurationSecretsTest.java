package com.abittech.platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

/**
 * Verifies that no secret is hard-coded in the committed configuration.
 */
class ConfigurationSecretsTest {

    private static final Pattern ENV_PLACEHOLDER = Pattern.compile("^\\$\\{[A-Z0-9_]+}$");
    private static final Pattern SENSITIVE_KEY =
            Pattern.compile(".*(password|secret|api[-_.]?key|token).*", Pattern.CASE_INSENSITIVE);

    private static Properties load(String file) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(file));
        Properties properties = yaml.getObject();
        assertThat(properties).as("%s must be on the classpath", file).isNotNull();
        return properties;
    }

    @ParameterizedTest
    @ValueSource(strings = {"application.yml", "application-dev.yml", "application-prod.yml"})
    void sensitiveKeysOnlyReferenceEnvironmentVariables(String file) {
        load(file).forEach((key, value) -> {
            if (SENSITIVE_KEY.matcher(key.toString()).matches()) {
                assertThat(value.toString())
                        .as("%s: %s must be an environment placeholder without a default", file, key)
                        .matches(ENV_PLACEHOLDER);
            }
        });
    }

    @Test
    void databaseSettingsComeFromEnvironmentVariables() {
        Properties properties = load("application.yml");
        assertThat(properties.getProperty("spring.datasource.url")).isEqualTo("${DATABASE_URL}");
        assertThat(properties.getProperty("spring.datasource.username")).isEqualTo("${DATABASE_USERNAME}");
        assertThat(properties.getProperty("spring.datasource.password")).isEqualTo("${DATABASE_PASSWORD}");
    }

    @Test
    void jwtSecretComesFromEnvironmentVariable() {
        Properties properties = load("application.yml");
        assertThat(properties.getProperty("app.jwt.secret")).isEqualTo("${JWT_SECRET}");
    }

    @Test
    void aiServiceKeyComesFromEnvironmentVariable() {
        Properties properties = load("application.yml");
        assertThat(properties.getProperty("app.ai.api-key")).isEqualTo("${AI_SERVICE_API_KEY}");
    }

    @Test
    void flywayCleanIsDisabled() {
        assertThat(load("application.yml").get("spring.flyway.clean-disabled")).isEqualTo(true);
    }

    @Test
    void envExampleContainsOnlyPlaceholders() throws IOException {
        Path envExample = Path.of(".env.example");
        assumeTrue(Files.exists(envExample), "run from the backend/ directory (Maven does this by default)");
        String content = Files.readString(envExample);
        assertThat(content).contains("DATABASE_URL=", "DATABASE_USERNAME=", "DATABASE_PASSWORD=your_password_here",
                "JWT_SECRET=replace_with", "JWT_EXPIRATION=", "AI_SERVICE_API_KEY=replace_with");
    }
}
