package com.aulaia.dto.estudiante;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada de creación/actualización de un estudiante
 * (contrato interno del Service; el contrato REST llega en Prompt 4.3).
 *
 * <p>{@code qrToken} NO forma parte de este contrato: se genera de forma
 * segura en el Service (Prompt 4.2). {@code activo} tampoco: el modelo
 * oficial (04-BD §6.5) define {@code activo BOOLEAN NOT NULL DEFAULT
 * TRUE} y la desactivación tiene su propia operación documentada
 * (07-PLAN 4.2 / 06-FLUJOS #48).
 *
 * <p>Longitudes según 04-BD §6.5: codigo VARCHAR(50), nombres y
 * apellidos VARCHAR(120).
 */
public record EstudianteRequest(
        @NotBlank(message = "codigo es obligatorio")
        @Size(max = 50, message = "codigo no puede exceder 50 caracteres")
        String codigo,

        @NotBlank(message = "nombres es obligatorio")
        @Size(max = 120, message = "nombres no puede exceder 120 caracteres")
        String nombres,

        @NotBlank(message = "apellidos es obligatorio")
        @Size(max = 120, message = "apellidos no puede exceder 120 caracteres")
        String apellidos,

        @NotNull(message = "seccionId es obligatorio")
        Long seccionId) {
}