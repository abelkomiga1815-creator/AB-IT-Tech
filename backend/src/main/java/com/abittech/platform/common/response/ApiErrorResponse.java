package com.abittech.platform.common.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard envelope for failed API responses.
 *
 * <pre>
 * {
 *   "success": false,
 *   "code": "RESOURCE_NOT_FOUND",
 *   "message": "The requested resource was not found",
 *   "timestamp": "2026-01-01T10:00:00Z"
 * }
 * </pre>
 *
 * @param success   always {@code false}
 * @param code      stable, machine-readable error code (see {@code ErrorCode})
 * @param message   safe, human-readable message; never contains stack traces or internals
 * @param timestamp moment the error was produced, in UTC
 * @param errors    field-level problems for validation failures, omitted otherwise
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        boolean success,
        String code,
        String message,
        Instant timestamp,
        List<FieldValidationError> errors) {
}
