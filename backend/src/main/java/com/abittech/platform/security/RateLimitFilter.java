package com.abittech.platform.security;

import java.io.IOException;
import java.time.Duration;

import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Slows down abuse of the public write endpoints: login/registration (password guessing, account
 * spam) and guest inquiries (spam). Limits are per client address and per minute.
 * <p>
 * The client address is the TCP peer address unless the application is configured to trust a reverse
 * proxy ({@code FORWARD_HEADERS_STRATEGY=framework}). Client addresses are never logged.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final SlidingWindowRateLimiter limiter;
    private final RateLimitProperties properties;
    private final RestErrorWriter errorWriter;

    public RateLimitFilter(SlidingWindowRateLimiter limiter, RateLimitProperties properties, RestErrorWriter errorWriter) {
        this.limiter = limiter;
        this.properties = properties;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (properties.enabled() && "POST".equals(request.getMethod())) {
            String bucket = bucketOf(request.getRequestURI());
            if (bucket != null) {
                int limit = "auth".equals(bucket) ? properties.authPerMinute() : properties.inquiryPerMinute();
                if (!limiter.tryAcquire(bucket + ":" + request.getRemoteAddr(), limit, WINDOW)) {
                    log.warn("Rate limit exceeded: bucket={}", bucket);
                    response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
                    errorWriter.write(response, ErrorCode.RATE_LIMITED);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private static String bucketOf(String path) {
        return switch (path) {
            case "/api/v1/auth/login", "/api/v1/auth/register" -> "auth";
            case "/api/v1/inquiries" -> "inquiry";
            default -> null;
        };
    }
}
