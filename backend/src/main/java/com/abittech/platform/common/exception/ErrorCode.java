package com.abittech.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable error codes returned to API clients, each with its HTTP status and a safe default message.
 * Clients should branch on the code, never on the message text. Keep docs/api/API-REFERENCE.md in sync.
 */
public enum ErrorCode {

    // 400
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "The request is invalid"),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "This role cannot be selected at registration"),
    INVALID_PROFILE_DATA(HttpStatus.BAD_REQUEST, "The profile data is invalid"),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "The uploaded file is missing or empty"),
    // 401 / 403
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    // 404 / 405 / 415
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found"),
    CUSTOMER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "Customer profile not found"),
    STUDENT_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "Student profile not found"),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "Project not found"),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Document not found"),
    DEFENSE_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Defense question not found"),
    RESEARCH_ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "Research analysis not found"),
    RESEARCH_SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Saved source not found"),
    SERVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Service not found"),
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "Inquiry not found"),
    SERVICE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Service request not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "The HTTP method is not supported for this resource"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request content type is not supported"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "This file type is not allowed"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "The file is too large"),
    // 409
    CONFLICT(HttpStatus.CONFLICT, "The request conflicts with existing data"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "An account with this email already exists"),
    CUSTOMER_PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "A customer profile already exists for this account"),
    STUDENT_PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "A student profile already exists for this account"),
    STUDENT_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "This student number is already registered for the university"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "The status cannot be changed this way"),
    RESEARCH_SOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "This source is already saved for the project"),
    // 429
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please wait a moment and try again"),
    ANALYSIS_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "A research analysis was run for this project a moment ago. Please wait a minute and try again"),
    // 500 / 502 / 503
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later"),
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "The research service returned an unexpected answer. Please try again later"),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The research service is not available right now. Please try again later");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
