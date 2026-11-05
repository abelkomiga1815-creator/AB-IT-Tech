package com.abittech.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shortSecretIsRejected() {
        assertThat(validator.validate(new JwtProperties("too-short", 1000))).isNotEmpty();
    }

    @Test
    void blankSecretIsRejected() {
        assertThat(validator.validate(new JwtProperties("", 1000))).isNotEmpty();
    }

    @Test
    void nonPositiveExpirationIsRejected() {
        assertThat(validator.validate(new JwtProperties("x".repeat(40), 0))).isNotEmpty();
    }

    @Test
    void longSecretAndPositiveExpirationAreAccepted() {
        assertThat(validator.validate(new JwtProperties("x".repeat(40), 1000))).isEmpty();
    }
}
