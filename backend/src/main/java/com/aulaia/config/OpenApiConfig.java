package com.aulaia.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración central de OpenAPI 3 + Swagger UI (docs/03-ARQUITECTURA #39).
 *
 * - Info básica de la API (sin datos personales ni URLs inventadas).
 * - Esquema de seguridad "bearerAuth" declarado conceptualmente para los
 *   endpoints del Sprint 2 (JWT). Es SOLO documentación: no exige token
 *   mientras no exista autenticación real.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aulaiaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AulaIA API")
                        .version("v1")
                        .description("""
                                API REST del Sistema Inteligente de Asistencia Escolar AulaIA.

                                Asistencia escolar, gestión académica, reportes e integración IA.
                                """))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}