package com.aulaia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj inyectable del sistema (Prompt 6.3 §6): {@link Clock} permite que
 * los servicios obtengan la hora oficial del servidor con
 * {@code OffsetDateTime.now(clock)} y que los tests inyecten un reloj fijo
 * determinista. Sin librerías externas ({@code java.time}).
 *
 * <p>Bean único de aplicación: por defecto {@link Clock#systemDefaultZone()}
 * (zona del servidor, coherente con TIMESTAMPTZ). Los tests lo reemplazan
 * con un {@code Clock} fijo cuando lo necesitan.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}