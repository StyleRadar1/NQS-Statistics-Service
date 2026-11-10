package edu.dosw.styleradar.estadisticas.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    void testOpenApiConfiguration() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.estadisticasOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("StyleRadar · Microservicio de Estadisticas", openAPI.getInfo().getTitle());
        assertEquals("v1.0", openAPI.getInfo().getVersion());
    }
}
