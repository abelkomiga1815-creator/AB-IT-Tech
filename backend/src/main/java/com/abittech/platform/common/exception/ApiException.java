package com.abittech.platform.common.exception;

/**
 * Base class for expected, client-facing business errors. The HTTP status and error code come from
 * the {@link ErrorCode}; the message must be safe to show to clients.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
