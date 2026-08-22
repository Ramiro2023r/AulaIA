package com.aulaia.controller;

import com.aulaia.client.fastapi.dto.FastApiAnalisisResponse;
import com.aulaia.dto.IaConsultaRequest;
import com.aulaia.dto.IaConsultaResponse;
import com.aulaia.service.IaIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Controlador para funcionalidades de Inteligencia Artificial (Prompt 17.1).
 * <p>
 * Expone endpoints para análisis estadístico y consultas en lenguaje natural.
 * Delega al servicio de integración que prepara datos y llama a FastAPI.
 */
@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
@Tag(name = "Inteligencia Artificial", description = "Análisis y consultas inteligentes de asistencia")
public class IaController {

    private final IaIntegrationService iaIntegrationService;

    @Operation(
            summary = "Consulta en lenguaje natural sobre asistencia",
            description = """
                    Procesa una pregunta del docente en lenguaje natural y devuelve
                    una respuesta fundamentada en los datos de asistencia autorizados.
                    
                    El flujo es:
                    1. Spring Boot valida permisos (solo clases del docente)
                    2. Obtiene datos filtrados según la pregunta
                    3. Envía a FastAPI para análisis
                    4. Devuelve respuesta con grounding en datos reales
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Respuesta generada",
                            content = @Content(schema = @Schema(implementation = IaConsultaResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Sin permisos para los datos consultados"),
                    @ApiResponse(responseCode = "503", description = "Servicio IA no disponible temporalmente")
            }
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    @PostMapping("/consulta")
    public ResponseEntity<IaConsultaResponse> consultar(
            @RequestHeader(value = "X-AI-Provider", defaultValue = "gemini") String aiProvider,
            @RequestHeader(value = "X-AI-Key", defaultValue = "") String apiKey,
            @RequestBody IaConsultaRequest request) {
        
        Optional<FastApiAnalisisResponse> analisis = iaIntegrationService.analizarAsistencia(
                buildFiltrosDesdeConsulta(request), request.getPregunta(), aiProvider, apiKey
        );

        if (analisis.isEmpty()) {
            return ResponseEntity.ok(IaConsultaResponse.builder()
                    .respuesta("El servicio de análisis inteligente no está disponible temporalmente. "
                            + "Por favor, intente nuevamente en unos minutos.")
                    .iaDisponible(false)
                    .build());
        }

        FastApiAnalisisResponse respuesta = analisis.get();
        String respuestaTexto;
        if (respuesta.getRespuestaIa() != null && !respuesta.getRespuestaIa().isBlank()) {
            respuestaTexto = respuesta.getRespuestaIa();
        } else {
            respuestaTexto = construirRespuestaTexto(request.getPregunta(), respuesta);
        }

        return ResponseEntity.ok(IaConsultaResponse.builder()
                .respuesta(respuestaTexto)
                .iaDisponible(true)
                .datosAnalisis(respuesta)
                .build());
    }

    @Operation(
            summary = "Genera resumen estadístico autorizado",
            description = """
                    Devuelve un resumen estadístico de asistencia para las clases del docente
                    (o globales si es ADMIN). Útil para dashboard y reportes rápidos.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    @GetMapping("/resumen")
    public ResponseEntity<IaConsultaResponse> resumen(
            @RequestHeader(value = "X-AI-Provider", defaultValue = "gemini") String aiProvider,
            @RequestHeader(value = "X-AI-Key", defaultValue = "") String apiKey) {
        
        // Filtros vacíos = datos autorizados según rol
        com.aulaia.dto.ReporteFiltrosDto filtros = new com.aulaia.dto.ReporteFiltrosDto();
        
        Optional<FastApiAnalisisResponse> analisis = iaIntegrationService.analizarAsistencia(
                filtros, "Genera un resumen general de mis clases basado en los datos estadísticos.", aiProvider, apiKey
        );

        if (analisis.isEmpty()) {
            return ResponseEntity.ok(IaConsultaResponse.builder()
                    .respuesta("No se pudo generar el resumen. El servicio IA no está disponible.")
                    .iaDisponible(false)
                    .build());
        }

        FastApiAnalisisResponse r = analisis.get();
        String texto;
        if (r.getRespuestaIa() != null && !r.getRespuestaIa().isBlank()) {
            texto = r.getRespuestaIa();
        } else {
            texto = String.format(
                    "Resumen de asistencia: %.1f%% global (%d estudiantes). " +
                    "Tendencia: %s. %s",
                    r.getResumenGeneral().getPorcentajeAsistenciaGlobal(),
                    r.getResumenGeneral().getTotalEstudiantes(),
                    r.getResumenGeneral().getTendenciaGlobal(),
                    r.getRecomendaciones().isEmpty() ? "" : r.getRecomendaciones().get(0)
            );
        }

        return ResponseEntity.ok(IaConsultaResponse.builder()
                .respuesta(texto)
                .iaDisponible(true)
                .datosAnalisis(r)
                .build());
    }

    @Operation(
            summary = "Health check del módulo IA",
            description = "Verifica si el servicio FastAPI está disponible"
    )
    @GetMapping("/health")
    public ResponseEntity<IaConsultaResponse> health() {
        boolean disponible = iaIntegrationService.isIaAvailable();
        
        return ResponseEntity.ok(IaConsultaResponse.builder()
                .respuesta(disponible ? "Servicio IA operativo" : "Servicio IA no disponible")
                .iaDisponible(disponible)
                .build());
    }

    // Métodos auxiliares privados

    private com.aulaia.dto.ReporteFiltrosDto buildFiltrosDesdeConsulta(IaConsultaRequest request) {
        com.aulaia.dto.ReporteFiltrosDto filtros = new com.aulaia.dto.ReporteFiltrosDto();
        // TODO: NLP básico para extraer filtros de la pregunta
        // Ejemplo: "¿Cómo estuvo 6A esta semana?" -> seccionId, fechaInicio/Fin
        return filtros;
    }

    private String construirRespuestaTexto(String pregunta, FastApiAnalisisResponse r) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Análisis de asistencia:\n\n");
        sb.append(String.format("• Estudiantes analizados: %d\n", 
                r.getResumenGeneral().getTotalEstudiantes()));
        sb.append(String.format("• Asistencia global: %.1f%%\n", 
                r.getResumenGeneral().getPorcentajeAsistenciaGlobal()));
        sb.append(String.format("• Tendencia: %s\n", 
                r.getResumenGeneral().getTendenciaGlobal()));
        
        if (!r.getInsightsEstudiantes().isEmpty()) {
            sb.append("\nInsights por estudiante:\n");
            for (var insight : r.getInsightsEstudiantes()) {
                sb.append(String.format("  - %s: %.1f%% (%s, %s)\n",
                        insight.getNombre(),
                        insight.getPorcentajeAsistencia(),
                        insight.getTendencia(),
                        insight.getNivelAtencion()));
                if (!insight.getObservaciones().isEmpty()) {
                    sb.append(String.format("    • %s\n", String.join("; ", insight.getObservaciones())));
                }
            }
        }
        
        if (!r.getPatronesDetectados().isEmpty()) {
            sb.append("\nPatrones detectados:\n");
            for (var p : r.getPatronesDetectados()) {
                sb.append(String.format("  ⚠ %s: %s\n", p.getTipo(), p.getDescripcion()));
            }
        }
        
        if (!r.getRecomendaciones().isEmpty()) {
            sb.append("\nRecomendaciones:\n");
            for (var rec : r.getRecomendaciones()) {
                sb.append(String.format("  • %s\n", rec));
            }
        }
        
        return sb.toString();
    }
}