package com.abittech.platform.common.validation;

/**
 * Regular expressions shared by request DTOs. Optional fields use them with {@code @Pattern},
 * which accepts {@code null}.
 */
public final class ValidationPatterns {

    /** International-friendly phone number: optional +, digits, spaces, dashes, parentheses. */
    public static final String PHONE = "^\\+?[0-9][0-9 ()-]{5,19}$";

    /** Student/registration numbers: letters, digits and / . _ - */
    public static final String STUDENT_NUMBER = "^[A-Za-z0-9/._-]{1,50}$";

    private ValidationPatterns() {
    }
}
