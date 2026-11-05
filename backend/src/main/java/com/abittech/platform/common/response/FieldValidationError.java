package com.abittech.platform.common.response;

/**
 * One validation problem, tied to the request field (or parameter) that caused it.
 */
public record FieldValidationError(String field, String message) {
}
