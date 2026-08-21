package com.aulaia.dto.justificacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JustificacionRequest(
        @NotNull(message = "El ID de la asistencia es obligatorio")
        Long asistenciaId,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500, message = "El motivo no puede exceder los 500 caracteres")
        String motivo
) {}
