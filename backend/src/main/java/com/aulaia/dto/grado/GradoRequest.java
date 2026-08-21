package com.aulaia.dto.grado;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request de creación/actualización de un grado (POST/PUT
 * /api/v1/grados).
 *
 * <p>{@code nivel} es opcional: si viene en blanco se usa el default del
 * modelo oficial {@code 'PRIMARIA'} (04-BD §6.3). {@code orden} es
 * opcional y nullable (SMALLINT del modelo oficial).
 */
public record GradoRequest(
        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 80, message = "nombre no puede exceder 80 caracteres")
        String nombre,

        @Size(max = 50, message = "nivel no puede exceder 50 caracteres")
        String nivel,

        @Min(value = 0, message = "orden no puede ser negativo")
        @Max(value = 32767, message = "orden no puede exceder 32767")
        Integer orden) {
}