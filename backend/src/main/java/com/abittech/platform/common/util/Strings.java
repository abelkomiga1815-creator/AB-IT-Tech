package com.abittech.platform.common.util;

import java.util.Locale;

/**
 * Small text helpers shared by services, so normalisation rules live in one place.
 */
public final class Strings {

    private Strings() {
    }

    /** Trims the value and converts a blank result to {@code null}. */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Canonical form of an e-mail address: trimmed and lower-cased. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
