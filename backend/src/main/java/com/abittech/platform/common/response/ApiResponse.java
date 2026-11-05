package com.abittech.platform.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard envelope for successful API responses.
 *
 * <pre>{"success": true, "data": {...}}</pre>
 *
 * @param success always {@code true} for this type; failures use {@link ApiErrorResponse}
 * @param data    response payload, omitted from the JSON when {@code null}
 * @param <T>     payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null);
    }
}
