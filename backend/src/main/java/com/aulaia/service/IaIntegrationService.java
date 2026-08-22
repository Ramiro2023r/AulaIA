package com.aulaia.service;

import com.aulaia.client.fastapi.FastApiClient;
import com.aulaia.client.fastapi.dto.FastApiAnalisisRequest;
import com.aulaia.client.fastapi.dto.FastApiAnalisisResponse;
import com.aulaia.client.fastapi.dto.FastApiAsistenciaEstudianteRequest;
import com.aulaia.client.fastapi.dto.FastApiHealthResponse;
import com.aulaia.dto.ReporteAsistenciaDto;
import com.aulaia.dto.ReporteFiltrosDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de integración con FastAPI IA (Prompt 16.5).
 * <p>
 * Prepara datasets controlados desde Spring Boot y los envía a FastAPI.
 * FastAPI NO consulta la BD directamente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IaIntegrationService {

    private final FastApiClient fastApiClient;
    private final ReporteService reporteService;

    /**
     * Verifica si el servicio IA está disponible.
     */
    public boolean isIaAvailable() {
        return fastApiClient.isAvailable();
    }

    /**
     * Health check del servicio IA.
     */
    public Optional<FastApiHealthResponse> healthCheck() {
        return fastApiClient.healthCheck();
    }

    /**
     * Genera análisis de asistencia para los filtros dados.
     * <p>
     * Obtiene datos del reporte, los transforma al formato FastAPI
     * y envía para análisis.
     *
     * @return Optional con análisis si IA disponible y responde OK,
     *         empty si IA no disponible o falla (no bloquea asistencia)
     */
    public Optional<FastApiAnalisisResponse> analizarAsistencia(ReporteFiltrosDto filtros, String pregunta, String aiProvider, String apiKey) {
        log.info("Solicitando análisis IA para filtros: {}", filtros);

        // 1. Obtener datos desde Spring Boot (fuente de verdad)
        List<ReporteAsistenciaDto> datos = reporteService.generarReporteAsistencias(filtros);

        if (datos.isEmpty()) {
            log.info("No hay datos de asistencia para analizar");
            return Optional.empty();
        }

        // 2. Agregar por estudiante (FastAPI espera datos agregados)
        List<FastApiAsistenciaEstudianteRequest> estudiantes = agregarPorEstudiante(datos);

        // 3. Construir request para FastAPI
        FastApiAnalisisRequest request = FastApiAnalisisRequest.builder()
                .pregunta(pregunta)
                .aiProvider(aiProvider)
                .apiKey(apiKey)
                .estudiantes(estudiantes)
                .periodo(filtros.getFechaInicio() + " a " + filtros.getFechaFin())
                .build();

        // 4. Enviar a FastAPI (con fallback silencioso)
        return fastApiClient.analizarAsistencia(request);
    }

    /**
     * Agrega datos de asistencia por estudiante para envío a IA.
     */
    private List<FastApiAsistenciaEstudianteRequest> agregarPorEstudiante(
            List<ReporteAsistenciaDto> datos) {

        return datos.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getEstudianteNombreCompleto(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                lista -> {
                                    ReporteAsistenciaDto first = lista.get(0);
                                    long presentes = lista.stream()
                                            .filter(d -> "PRESENTE".equals(d.getEstadoAsistencia()))
                                            .count();
                                    long tardanzas = lista.stream()
                                            .filter(d -> "TARDANZA".equals(d.getEstadoAsistencia()))
                                            .count();
                                    long ausentes = lista.stream()
                                            .filter(d -> "AUSENTE".equals(d.getEstadoAsistencia()))
                                            .count();
                                    long justificados = lista.stream()
                                            .filter(d -> "JUSTIFICADO".equals(d.getEstadoAsistencia()))
                                            .count();

                                    return FastApiAsistenciaEstudianteRequest.builder()
                                            .estudianteId(first.getEstudianteId() != null ? first.getEstudianteId() : 0L)
                                            .nombre(first.getEstudianteNombreCompleto())
                                            .presentes((int) presentes)
                                            .tardanzas((int) tardanzas)
                                            .ausentes((int) ausentes)
                                            .justificados((int) justificados)
                                            .totalSesiones(lista.size())
                                            .build();
                                }
                        )
                ))
                .values()
                .stream()
                .toList();
    }
}