package com.abittech.platform.common.exception;

/**
 * Generic "entity does not exist" error. Mapped to HTTP 404 {@code RESOURCE_NOT_FOUND}.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
