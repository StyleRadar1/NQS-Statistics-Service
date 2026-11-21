package edu.dosw.styleradar.estadisticas.infrastructure.exception;

import edu.dosw.styleradar.estadisticas.domain.exception.InvalidTimeSeriesRequestException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/analytics/time-series");
    }

    @Test
    @DisplayName("Debe manejar UnauthorizedScopeException retornando 403")
    void testHandleUnauthorizedScope() {
        UnauthorizedScopeException ex = new UnauthorizedScopeException("No autorizado");
        ResponseEntity<Map<String, Object>> response = handler.handleUnauthorizedScope(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().get("status"));
        assertEquals("No autorizado", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Debe manejar UnsupportedMetricScopeException retornando 400")
    void testHandleUnsupportedMetricScope() {
        UnsupportedMetricScopeException ex = new UnsupportedMetricScopeException("Scope no soportado");
        ResponseEntity<Map<String, Object>> response = handler.handleUnsupportedMetricScope(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Scope no soportado", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Debe manejar InvalidTimeSeriesRequestException retornando 400")
    void testHandleInvalidTimeSeriesRequest() {
        InvalidTimeSeriesRequestException ex = new InvalidTimeSeriesRequestException("Rango inválido");
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidTimeSeriesRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Rango inválido", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Debe manejar IllegalArgumentException retornando 400")
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Argumento inválido");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Argumento inválido", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Debe manejar MethodArgumentTypeMismatchException retornando 400")
    void testHandleTypeMismatch() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("abc", Integer.class, "id", null, null);
        ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("id"));
    }

    @Test
    @DisplayName("Debe manejar Exception general retornando 500")
    void testHandleGeneralException() {
        Exception ex = new RuntimeException("Error fatal");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneralException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("Error fatal"));
    }
}
