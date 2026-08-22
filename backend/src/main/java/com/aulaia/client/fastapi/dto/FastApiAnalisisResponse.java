package com.aulaia.client.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response del análisis de asistencia desde FastAPI (Prompt 16.5).
 * <p>
 * Coincide con {@code AnalisisResponse} en FastAPI schemas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastApiAnalisisResponse {

    @JsonProperty("respuesta_ia")
    private String respuestaIa;

    @JsonProperty("resumen_general")
    private ResumenGeneral resumenGeneral;

    @JsonProperty("insights_estudiantes")
    private List<InsightEstudiante> insightsEstudiantes;

    @JsonProperty("patrones_detectados")
    private List<PatronDetectado> patronesDetectados;

    private List<String> recomendaciones;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenGeneral {
        @JsonProperty("total_estudiantes")
        private Integer totalEstudiantes;

        @JsonProperty("total_sesiones")
        private Integer totalSesiones;

        @JsonProperty("total_presentes")
        private Integer totalPresentes;

        @JsonProperty("total_tardanzas")
        private Integer totalTardanzas;

        @JsonProperty("total_ausentes")
        private Integer totalAusentes;

        @JsonProperty("total_justificados")
        private Integer totalJustificados;

        @JsonProperty("porcentaje_asistencia_global")
        private Double porcentajeAsistenciaGlobal;

        @JsonProperty("tendencia_global")
        private String tendenciaGlobal;

        private String periodo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightEstudiante {
        @JsonProperty("estudiante_id")
        private Integer estudianteId;

        private String nombre;

        @JsonProperty("porcentaje_asistencia")
        private Double porcentajeAsistencia;

        private String tendencia;

        @JsonProperty("nivel_atencion")
        private String nivelAtencion;

        private List<String> observaciones;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatronDetectado {
        private String tipo;
        private String descripcion;

        @JsonProperty("estudiantes_afectados")
        private List<Integer> estudiantesAfectados;

        private String severidad;
    }
}