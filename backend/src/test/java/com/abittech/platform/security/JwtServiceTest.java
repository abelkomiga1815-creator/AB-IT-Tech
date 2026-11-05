package com.abittech.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import com.abittech.platform.config.JwtProperties;
import com.abittech.platform.user.entity.Role;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz";
    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    private JwtService service(String secret, long expirationMs, Instant now) {
        return new JwtService(new JwtProperties(secret, expirationMs), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void generatedTokenCanBeParsedBackToTheUserId() {
        JwtService service = service(SECRET, 60_000, NOW);
        UUID userId = UUID.randomUUID();

        String token = service.generateToken(userId, "a@example.com", Role.STUDENT);

        assertThat(service.parseUserId(token)).contains(userId);
    }

    @Test
    void tokenPayloadHoldsOnlyExpectedClaimsAndNoPassword() {
        String token = service(SECRET, 60_000, NOW).generateToken(UUID.randomUUID(), "a@example.com", Role.CUSTOMER);

        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertThat(payload).contains("\"sub\"", "\"email\"", "\"role\"", "\"iat\"", "\"exp\"");
        assertThat(payload.toLowerCase()).doesNotContain("password");
    }

    @Test
    void expiredTokenIsRejected() {
        String token = service(SECRET, 60_000, NOW).generateToken(UUID.randomUUID(), "a@example.com", Role.STUDENT);

        JwtService later = service(SECRET, 60_000, NOW.plus(Duration.ofMinutes(5)));

        assertThat(later.parseUserId(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String foreign = service("another-secret-0123456789-abcdefghijklmnopqrstuvwxyz", 60_000, NOW)
                .generateToken(UUID.randomUUID(), "a@example.com", Role.STUDENT);

        assertThat(service(SECRET, 60_000, NOW).parseUserId(foreign)).isEmpty();
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = service(SECRET, 60_000, NOW);
        String token = service.generateToken(UUID.randomUUID(), "a@example.com", Role.STUDENT);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThat(service.parseUserId(tampered)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        JwtService service = service(SECRET, 60_000, NOW);

        assertThat(service.parseUserId("not-a-jwt")).isEmpty();
        assertThat(service.parseUserId("")).isEmpty();
    }

    @Test
    void expirationSecondsReflectsConfiguration() {
        assertThat(service(SECRET, 86_400_000, NOW).expirationSeconds()).isEqualTo(86_400);
    }
}
