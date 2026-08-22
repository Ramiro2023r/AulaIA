package com.aulaia.dto.estudiante;

import com.aulaia.entity.Parentesco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para registrar un apoderado y asociarlo directamente a un estudiante. */
public record ApoderadoEstudianteRequest(
        @NotBlank(message = "nombres es obligatorio")
        @Size(max = 120, message = "nombres no puede exceder 120 caracteres")
        String nombres,

        @NotBlank(message = "apellidos es obligatorio")
        @Size(max = 120, message = "apellidos no puede exceder 120 caracteres")
        String apellidos,

        @Size(max = 30, message = "telefono no puede exceder 30 caracteres")
        String telefono,

        @NotNull(message = "parentesco es obligatorio")
        Parentesco parentesco,

        boolean principal) {
}
