package com.abittech.platform.common.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request a correlation id and writes one access-style log line per request.
 * <ul>
 *   <li>The id is taken from the {@code X-Request-Id} header when it is a safe value,
 *       otherwise a random UUID is generated.</li>
 *   <li>The id is stored in the SLF4J MDC (key {@code requestId}, printed in every log line)
 *       and returned in the {@code X-Request-Id} response header.</li>
 *   <li>Only method, path, status and duration are logged. Query strings, headers, cookies and
 *       bodies are never logged, so passwords, tokens and personal data cannot leak here.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER_NAME));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);
        long startNanos = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - startNanos) / 1_000_000;
            logCompletion(request, response, millis);
            MDC.remove(MDC_KEY);
        }
    }

    private static String resolveRequestId(String candidate) {
        if (candidate != null && SAFE_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private static void logCompletion(HttpServletRequest request, HttpServletResponse response, long millis) {
        String path = request.getRequestURI();
        boolean noisy = path.startsWith("/actuator") || path.equals("/api/v1/health");
        if (noisy && !log.isDebugEnabled()) {
            return;
        }
        if (noisy) {
            log.debug("{} {} -> {} ({} ms)", request.getMethod(), path, response.getStatus(), millis);
        } else {
            log.info("{} {} -> {} ({} ms)", request.getMethod(), path, response.getStatus(), millis);
        }
    }
}
