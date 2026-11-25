package edu.dosw.styleradar.estadisticas.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI estadisticasOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("StyleRadar · Statistics Microservice")
                .description("StyleRadar Statistics API (Package 4). Exposes metrics by scope, period, and metric code.")
                .version("v1.0")
                .contact(new Contact()
                    .name("DOSW Team")
                    .email("dosw@eci.edu.co")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
            .components(new Components()
                .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                    .name(BEARER_AUTH_SCHEME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token issued by ms-auth. Paste the token directly (without the 'Bearer ' prefix).")));
    }
}
