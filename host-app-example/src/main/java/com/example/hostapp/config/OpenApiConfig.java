package com.example.hostapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI hostAppOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Workflow Host API")
                .description("REST API for the workflow engine and host application. "
                    + "Use HTTP Basic (same users as the application) or authorize in Swagger UI.")
                .version("1.0"))
            .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
            .components(new Components().addSecuritySchemes(BASIC_AUTH,
                new SecurityScheme()
                    .name(BASIC_AUTH)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("basic")));
    }
}
