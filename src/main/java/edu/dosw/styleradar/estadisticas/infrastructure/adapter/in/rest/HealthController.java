package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health Check", description = "Verificación servicio activo")
public class HealthController {

    @GetMapping
    @Operation(summary = "Verificacion Servicio", description = "Verifica que el microservicio esté en ejecución y respondiendo peticiones.")
    @ApiResponse(responseCode = "200", description = "Health Check OK")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "ms-estadisticas",
            "port", 8091,
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
