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
@Tag(name = "Service Health", description = "Microservice availability verification")
public class HealthController {

    @GetMapping
    @Operation(summary = "Check availability", description = "Verifies that the microservice is running and responding to requests.")
    @ApiResponse(responseCode = "200", description = "The microservice is available")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "ms-estadisticas",
            "port", 8420,
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
