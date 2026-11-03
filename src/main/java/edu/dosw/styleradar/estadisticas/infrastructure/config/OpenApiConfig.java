package edu.dosw.styleradar.estadisticas.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI estadisticasOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("StyleRadar · Microservicio de Estadisticas")
                .description("Microservicio de Estadisticas de StyleRadar (Paquete 4). Gestiona el servicio de estadisticas para los usuarios")
                .version("v1.0")
                .contact(new Contact()
                    .name("Equipo DOSW")
                    .email("dosw@eci.edu.co")));
    }
}
