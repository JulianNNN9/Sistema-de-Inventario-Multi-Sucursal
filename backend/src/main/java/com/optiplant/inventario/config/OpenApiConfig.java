package com.optiplant.inventario.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentación OpenAPI (Módulo 9 · Fase A) y el esquema de
 * seguridad JWT que Swagger UI usa para el botón "Authorize". No reemplaza
 * ninguna regla de seguridad real: solo describe, en la documentación, el
 * mismo esquema Bearer que ya exige {@code SecurityConfig}.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI inventarioOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OptiPlant · API de Inventario Multi-Sucursal")
                        .version("v1")
                        .description("""
                                API REST del sistema de inventario multi-sucursal de OptiPlant Consultores. \
                                Toda ruta vive bajo el prefijo `/api/v1` y, salvo el login, exige un token \
                                JWT (`Authorization: Bearer <token>`) obtenido en `POST /api/v1/auth/login`. \
                                Cada operación documenta explícitamente qué rol(es) puede invocarla; ausencia \
                                de un rol autorizado responde HTTP 403.""")
                        .contact(new Contact().name("OptiPlant Consultores")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtenido en POST /api/v1/auth/login.")));
    }
}
