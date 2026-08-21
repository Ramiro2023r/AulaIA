package com.aulaia.dto.seccion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request de creación/actualización de una sección (POST/PUT
 * /api/v1/secciones).
 *
 * <p>{@code activo} NO forma parte del contrato de escritura: el modelo
 * oficial (04-BD §6.4) define {@code activo BOOLEAN NOT NULL DEFAULT TRUE}
 * y el módulo no tiene operación de desactivación (criterio consistente con
 * Grados), por lo que la sección se crea siempre activa.
 *
 * <p>{@code nombre} y {@code periodoAcademico} son VARCHAR(20) según el
 * modelo oficial; se recortan (trim) en el servicio.
 */
public record SeccionRequest(
        @NotNull(message = "gradoId es obligatorio")
        Long gradoId,

        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 20, message = "nombre no puede exceder 20 caracteres")
        String nombre,

        @NotBlank(message = "periodoAcademico es obligatorio")
        @Size(max = 20, message = "periodoAcademico no puede exceder 20 caracteres")
        String periodoAcademico) {
}
