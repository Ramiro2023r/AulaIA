package com.aulaia.dto.justificacion;

import com.aulaia.entity.EstadoJustificacion;
import jakarta.validation.constraints.NotNull;

public record EvaluarJustificacionRequest(
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoJustificacion estado
) {}
