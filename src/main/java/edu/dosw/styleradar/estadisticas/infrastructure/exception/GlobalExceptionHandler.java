package edu.dosw.styleradar.estadisticas.infrastructure.exception;

import edu.dosw.styleradar.estadisticas.domain.exception.InvalidTimeSeriesRequestException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_TYPE_PREFIX = "https://api.styleradar.com/errors/";

    @ExceptionHandler(UnauthorizedScopeException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedScope(
            UnauthorizedScopeException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(UnsupportedMetricScopeException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMetricScope(
            UnsupportedMetricScopeException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, "UNSUPPORTED_METRIC_SCOPE", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(InvalidTimeSeriesRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTimeSeriesRequest(
            InvalidTimeSeriesRequestException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_DATA", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String mensaje = String.format("Parámetro inválido '%s': valor '%s'", ex.getName(), ex.getValue());
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", mensaje, request.getRequestURI(), null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String mensaje = String.format("El parámetro obligatorio '%s' es requerido", ex.getParameterName());
        return buildResponse(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", mensaje, request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_DATA",
                "La solicitud contiene datos inválidos",
                request.getRequestURI(),
                fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Error interno inesperado: " + ex.getMessage(),
                request.getRequestURI(),
                null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String code,
            String mensaje,
            String path,
            Map<String, String> fieldErrors
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", ERROR_TYPE_PREFIX + code.toLowerCase(Locale.ROOT).replace('_', '-'));
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", mensaje);
        body.put("instance", path);
        body.put("code", code);
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            body.put("errors", fieldErrors);
        }
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("error", status.getReasonPhrase());
        body.put("message", mensaje);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }
}
