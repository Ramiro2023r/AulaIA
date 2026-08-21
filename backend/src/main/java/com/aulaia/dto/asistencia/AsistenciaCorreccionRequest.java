package com.aulaia.dto.asistencia;

import com.aulaia.entity.EstadoAsistencia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para la corrección/justificación manual de asistencia (Prompt 14.2).
 */
public record AsistenciaCorreccionRequest(
        @NotNull(message = "nuevoEstado es obligatorio")
        @Schema(description = "Nuevo estado de asistencia (PRESENTE, TARDE, FALTA, JUSTIFICADO)")
        EstadoAsistencia nuevoEstado,

        @NotBlank(message = "motivo es obligatorio")
        @Size(max = 255, message = "motivo no puede exceder 255 caracteres")
        @Schema(description = "Motivo obligatorio para la corrección manual (07-PLAN 14.2)")
        String motivo) {
}
