package com.abittech.platform.common.exception;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.abittech.platform.common.response.ApiErrorResponse;
import com.abittech.platform.common.response.FieldValidationError;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Converts every exception thrown while handling a request into the standard
 * {@link ApiErrorResponse} JSON.
 * <p>
 * Security rules: client messages are fixed or developer-authored, request values are never
 * echoed back, and stack traces are only written to the server log.
 * Errors raised by the security filters (401/403 before a controller is reached) are written by
 * the classes in the {@code security} package using the same format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex) {
        ErrorCode code = ex.getErrorCode();
        log.warn("Request rejected: {} ({})", code, code.status().value());
        return build(code.status(), code, ex.getMessage(), null, HttpHeaders.EMPTY);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestBodyValidation(MethodArgumentNotValidException ex) {
        List<FieldValidationError> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.add(new FieldValidationError(e.getField(), e.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> errors.add(new FieldValidationError(e.getObjectName(), e.getDefaultMessage())));
        log.warn("Request body validation failed with {} error(s)", errors.size());
        return build(ErrorCode.VALIDATION_ERROR.status(), ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.defaultMessage(), errors, HttpHeaders.EMPTY);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldValidationError> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldValidationError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        log.warn("Constraint validation failed with {} error(s)", errors.size());
        return build(ErrorCode.VALIDATION_ERROR.status(), ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.defaultMessage(), errors, HttpHeaders.EMPTY);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        log.warn("Request parameter validation failed");
        return build(ErrorCode.VALIDATION_ERROR.status(), ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.defaultMessage(), null, HttpHeaders.EMPTY);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // The exception message can quote parts of the payload, so it is deliberately not logged or returned.
        log.warn("Malformed or unreadable request body");
        return build(ErrorCode.BAD_REQUEST.status(), ErrorCode.BAD_REQUEST,
                "Malformed or unreadable request body", null, HttpHeaders.EMPTY);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid value for request parameter '{}'", ex.getName());
        return build(ErrorCode.BAD_REQUEST.status(), ErrorCode.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'", null, HttpHeaders.EMPTY);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Typically a race on a unique constraint. The SQL/constraint text is logged by class only.
        log.warn("Data integrity violation ({})", ex.getClass().getSimpleName());
        return build(ErrorCode.CONFLICT.status(), ErrorCode.CONFLICT,
                ErrorCode.CONFLICT.defaultMessage(), null, HttpHeaders.EMPTY);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("Upload rejected: file too large");
        return build(ErrorCode.FILE_TOO_LARGE.status(), ErrorCode.FILE_TOO_LARGE,
                ErrorCode.FILE_TOO_LARGE.defaultMessage(), null, HttpHeaders.EMPTY);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied");
        return build(ErrorCode.FORBIDDEN.status(), ErrorCode.FORBIDDEN,
                ErrorCode.FORBIDDEN.defaultMessage(), null, HttpHeaders.EMPTY);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication required");
        return build(ErrorCode.UNAUTHORIZED.status(), ErrorCode.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED.defaultMessage(), null, HttpHeaders.EMPTY);
    }

    /**
     * Fallback for everything else. Client errors raised by Spring MVC itself (unknown URL,
     * unsupported method, missing parameter, ...) keep their HTTP status; anything unexpected
     * becomes a generic 500 and its details stay in the server log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        if (ex instanceof ErrorResponse springError && springError.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = springError.getStatusCode();
            ErrorCode code = switch (status.value()) {
                case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
                case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
                case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
                default -> ErrorCode.BAD_REQUEST;
            };
            log.warn("Request rejected by Spring MVC: {} ({})", status.value(), ex.getClass().getSimpleName());
            return build(status, code, code.defaultMessage(), null, springError.getHeaders());
        }
        log.error("Unhandled exception while processing request", ex);
        return build(ErrorCode.INTERNAL_ERROR.status(), ErrorCode.INTERNAL_ERROR,
                ErrorCode.INTERNAL_ERROR.defaultMessage(), null, HttpHeaders.EMPTY);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatusCode status, ErrorCode code, String message,
                                                   List<FieldValidationError> errors, HttpHeaders headers) {
        ApiErrorResponse body = new ApiErrorResponse(false, code.name(), message, Instant.now(clock), errors);
        return ResponseEntity.status(status).headers(headers).body(body);
    }
}
