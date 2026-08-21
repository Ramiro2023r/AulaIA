package com.aulaia.dto.curso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request de creación/actualización de un curso (POST/PUT
 * /api/v1/cursos).
 *
 * <p>{@code activo} NO forma parte del contrato de escritura: el modelo
 * oficial (04-BD §6.6) define {@code activo BOOLEAN NOT NULL DEFAULT TRUE}
 * y el módulo no tiene operación de desactivación (criterio consistente con
 * Grados y Secciones), por lo que el curso se crea siempre activo.
 *
 * <p>{@code nombre} es VARCHAR(100) según el modelo oficial; {@code
 * descripcion} es VARCHAR(255) NULL (opcional, puede venir null o
 * ausente). Ambos se recortan (trim) en el servicio.
 */
public record CursoRequest(
        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 100, message = "nombre no puede exceder 100 caracteres")
        String nombre,

        @Size(max = 255, message = "descripcion no puede exceder 255 caracteres")
        String descripcion) {
}