package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI configuration.
 *
 * <p>Exposes interactive API docs at:
 * <ul>
 *   <li>Swagger UI: <a href="http://localhost:8080/swagger-ui.html">/swagger-ui.html</a></li>
 *   <li>OpenAPI JSON: <a href="http://localhost:8080/v3/api-docs">/v3/api-docs</a></li>
 * </ul>
 *
 * <p>A global {@code bearerAuth} security scheme is declared so the "Authorize"
 * button in Swagger UI lets you paste a JWT obtained from {@code POST /login}.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI emiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Secure EMI Loan Management API")
                        .description("Spring Boot backend for an NBFC: loans, EMI payments, "
                                + "penalties, dashboard analytics, JWT security and role-based access.")
                        .version("v1.0.0")
                        .contact(new Contact().name("EMI Project").email("support@nbfc.example"))
                        .license(new License().name("Apache 2.0")))
                // Apply JWT auth globally to all operations
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT returned by POST /login")));
    }
}

