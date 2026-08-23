package com.aulaia.dto.estudiante;

import com.aulaia.entity.Parentesco;
import jakarta.validation.constraints.NotNull;

/** Datos de la relación entre un estudiante y un apoderado ya registrado. */
public record AsociarApoderadoRequest(
        @NotNull(message = "parentesco es obligatorio")
        Parentesco parentesco,

        boolean principal) {
}
