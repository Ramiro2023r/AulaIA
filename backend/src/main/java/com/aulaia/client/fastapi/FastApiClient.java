package com.aulaia.client.fastapi;

import com.aulaia.client.fastapi.dto.FastApiAnalisisRequest;
import com.aulaia.client.fastapi.dto.FastApiAnalisisResponse;
import com.aulaia.client.fastapi.dto.FastApiHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

/**
 * Cliente para comunicarse con el servicio FastAPI de IA (Prompt 16.5).
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Health check del servicio IA</li>
 *   <li>Enviar datos agregados para análisis</li>
 *   <li>Manejar fallos sin bloquear el sistema principal</li>
 * </ul>
 *
 * <p><strong>Regla crítica:</strong> Si FastAPI falla, el registro de asistencia
 * NO debe afectarse. Solo se degrada el módulo IA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiClient {

    private final RestClient fastApiRestClient;
    private final FastApiProperties properties;

    /**
     * Verifica si el servicio FastAPI está disponible.
     *
     * @return Optional con health response si está OK, empty si falla
     */
    public Optional<FastApiHealthResponse> healthCheck() {
        try {
            FastApiHealthResponse response = fastApiRestClient
                    .get()
                    .uri("/api/v1/analisis/health")
                    .retrieve()
                    .body(FastApiHealthResponse.class);

            if (response != null && "ok".equalsIgnoreCase(response.getStatus())) {
                log.debug("FastAPI health check OK: {}", response);
                return Optional.of(response);
            }
            log.warn("FastAPI health check devolvió estado no OK: {}", response);
            return Optional.empty();

        } catch (RestClientException e) {
            log.warn("FastAPI no disponible (health check): {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Envía datos de asistencia para análisis estadístico.
     *
     * @param request datos agregados de asistencia
     * @return Optional con análisis si éxito, empty si falla
     */
    public Optional<FastApiAnalisisResponse> analizarAsistencia(FastApiAnalisisRequest request) {
        try {
            FastApiAnalisisResponse response = fastApiRestClient
                    .post()
                    .uri("/api/v1/analisis/asistencia")
                    .body(request)
                    .retrieve()
                    .body(FastApiAnalisisResponse.class);

            log.info("Análisis IA completado: {} estudiantes, {} patrones",
                    request.getEstudiantes().size(),
                    response != null ? response.getPatronesDetectados().size() : 0);

            return Optional.ofNullable(response);

        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            log.error("FastAPI error HTTP {}: {}", status, e.getResponseBodyAsString());
            return Optional.empty();

        } catch (RestClientException e) {
            log.warn("FastAPI no disponible (analizarAsistencia): {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Verifica disponibilidad rápida (para uso en UI).
     *
     * @return true si responde OK en menos de 2s
     */
    public boolean isAvailable() {
        return healthCheck()
                .map(r -> "ok".equalsIgnoreCase(r.getStatus()))
                .orElse(false);
    }
}