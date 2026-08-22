package com.aulaia.client.fastapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Propiedades de configuración para el cliente FastAPI (Prompt 16.5).
 * <p>
 * Prefijo: {@code aulaia.fastapi}
 * <ul>
 *   <li>{@code base-url} — URL base del servicio FastAPI (ej: http://localhost:8000)</li>
 *   <li>{@code connect-timeout} — Timeout de conexión (default: 5s)</li>
 *   <li>{@code read-timeout} — Timeout de lectura (default: 30s)</li>
 * </ul>
 */
@Component
@Validated
@ConfigurationProperties(prefix = "aulaia.fastapi")
public class FastApiProperties {

    @NotBlank
    private String baseUrl;

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(30);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}