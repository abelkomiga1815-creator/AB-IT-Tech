package com.abittech.platform.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();
    private final AtomicReference<String> idSeenByChain = new AtomicReference<>();
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() throws Exception {
        request = new MockHttpServletRequest("GET", "/api/v1/health");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            idSeenByChain.set(MDC.get(RequestIdFilter.MDC_KEY));
            return null;
        }).when(chain).doFilter(any(), any());
    }

    @Test
    void keepsSafeClientSuppliedId() throws Exception {
        request.addHeader(RequestIdFilter.HEADER_NAME, "client-id-12345");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(idSeenByChain.get()).isEqualTo("client-id-12345");
        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("client-id-12345");
    }

    @Test
    void replacesUnsafeClientSuppliedId() throws Exception {
        request.addHeader(RequestIdFilter.HEADER_NAME, "bad id with spaces and more");

        filter.doFilter(request, response, chain);

        String generated = response.getHeader(RequestIdFilter.HEADER_NAME);
        assertThat(generated).isNotNull().matches("^[0-9a-f-]{36}$");
        assertThat(idSeenByChain.get()).isEqualTo(generated);
    }

    @Test
    void generatesIdWhenMissingAndClearsMdcAfterwards() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isNotBlank();
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
