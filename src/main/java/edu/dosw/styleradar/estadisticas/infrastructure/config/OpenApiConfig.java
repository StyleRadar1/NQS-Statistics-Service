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
                .title("StyleRadar · Statistics Microservice")
                .description("StyleRadar Statistics API (Package 4). Exposes metrics by scope, period, and metric code.")
                .version("v1.0")
                .contact(new Contact()
                    .name("DOSW Team")
                    .email("dosw@eci.edu.co")));
    }
}
