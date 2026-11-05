package com.abittech.platform.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.common.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes the standard error JSON from inside security filters, where
 * {@code GlobalExceptionHandler} is not reachable.
 */
@Component
public class RestErrorWriter {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RestErrorWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiErrorResponse body = new ApiErrorResponse(false, code.name(), code.defaultMessage(), Instant.now(clock), null);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
