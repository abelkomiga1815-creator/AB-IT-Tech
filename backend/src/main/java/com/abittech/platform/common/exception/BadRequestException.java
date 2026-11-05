package com.abittech.platform.common.exception;

/**
 * A well-formed request that cannot be accepted. Mapped to HTTP 400 {@code BAD_REQUEST}.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }
}
