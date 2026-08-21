package com.aulaia.dto.seccion;

import java.time.OffsetDateTime;

/**
 * Response de una sección (GET/POST/PUT /api/v1/secciones).
 *
 * <p>Incluye únicamente un resumen mínimo del grado ({@link GradoResumen}:
 * id y nombre); no se expone la entidad {@code Grado} completa ni
 * estructuras anidadas excesivas. Timestamps según el modelo oficial
 * (04-BD §6.4): {@code created_at} y {@code updated_at}.
 */
public record SeccionResponse(
        Long id,
        GradoResumen grado,
        String nombre,
        String periodoAcademico,
        boolean activo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public record GradoResumen(Long id, String nombre) {
    }
}
