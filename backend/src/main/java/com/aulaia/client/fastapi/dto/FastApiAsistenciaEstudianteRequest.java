package com.aulaia.client.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para enviar datos de un estudiante al servicio FastAPI (Prompt 16.5).
 * <p>
 * Debe coincidir con {@code AsistenciaEstudianteDTO} en FastAPI schemas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastApiAsistenciaEstudianteRequest {

    @NotNull
    @JsonProperty("estudiante_id")
    private Long estudianteId;

    @NotBlank
    private String nombre;

    @Min(0)
    @JsonProperty("presentes")
    private Integer presentes;

    @Min(0)
    @JsonProperty("tardanzas")
    private Integer tardanzas;

    @Min(0)
    @JsonProperty("ausentes")
    private Integer ausentes;

    @Min(0)
    @JsonProperty("justificados")
    private Integer justificados;

    @Min(0)
    @JsonProperty("total_sesiones")
    private Integer totalSesiones;
}