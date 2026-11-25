package edu.dosw.styleradar.estadisticas.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void preservesIncomingCorrelationIdAndCleansMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/statistics/summary");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-stats-42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcValue = new AtomicReference<>();

        FilterChain chain = (req, res) -> mdcValue.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        filter.doFilter(request, response, chain);

        assertEquals("corr-stats-42", mdcValue.get());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
        assertEquals("corr-stats-42", response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        assertEquals("corr-stats-42", response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER));
    }

    @Test
    void generatesSafeCorrelationIdWhenMissingOrInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/statistics/summary");
        request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, "bad\r\nheader");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String generated = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(generated);
        assertFalse(generated.isBlank());
        assertFalse(generated.contains("\r"));
        assertFalse(generated.contains("\n"));
        assertEquals(generated, response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
