package com.aulaia.dto.grado;

import java.time.OffsetDateTime;

/**
 * Respuesta de un grado. Solo campos del recurso {@code grados}: nunca
 * información de usuarios, JWT ni detalles internos de JPA.
 */
public record GradoResponse(
        Long id,
        String nombre,
        String nivel,
        Integer orden,
        boolean activo,
        OffsetDateTime createdAt) {
}