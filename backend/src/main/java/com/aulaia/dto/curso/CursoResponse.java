package com.aulaia.dto.curso;

import java.time.OffsetDateTime;

/**
 * Response de un curso (GET/POST/PUT /api/v1/cursos).
 *
 * <p>Incluye únicamente campos de {@code Curso} definidos para API
 * (04-BD §6.6): id, nombre, descripcion, activo y timestamps. No se
 * exponen entidades internas ni estructuras futuras (Horarios, etc.).
 */
public record CursoResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean activo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}