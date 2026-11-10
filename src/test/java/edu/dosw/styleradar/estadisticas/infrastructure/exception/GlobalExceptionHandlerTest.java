package edu.dosw.styleradar.estadisticas.infrastructure.exception;

import edu.dosw.styleradar.estadisticas.domain.exception.InvalidPeriodoException;
import edu.dosw.styleradar.estadisticas.domain.exception.TableroNotFoundException;
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
        request.setRequestURI("/api/v1/estadisticas/tableros/TIENDA/123");
    }

    @Test
    @DisplayName("Debe manejar InvalidPeriodoException retornando 400")
    void testHandleInvalidPeriodo() {
        InvalidPeriodoException ex = new InvalidPeriodoException("Periodo inválido");
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidPeriodo(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Periodo inválido", response.getBody().get("message"));
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
    @DisplayName("Debe manejar TableroNotFoundException retornando 404")
    void testHandleTableroNotFound() {
        TableroNotFoundException ex = new TableroNotFoundException("No encontrado");
        ResponseEntity<Map<String, Object>> response = handler.handleTableroNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("No encontrado", response.getBody().get("message"));
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
