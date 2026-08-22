package com.aulaia.client.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request completo para el endpoint de análisis de FastAPI (Prompt 16.5).
 * <p>
 * Coincide con {@code AnalisisRequest} en FastAPI schemas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastApiAnalisisRequest {

    private String pregunta;

    @JsonProperty("ai_provider")
    private String aiProvider;

    @JsonProperty("ai_key")
    private String apiKey;

    @NotEmpty
    @Size(max = 5000)
    @Valid
    @JsonProperty("estudiantes")
    private List<FastApiAsistenciaEstudianteRequest> estudiantes;

    @JsonProperty("periodo")
    private String periodo;
}