package com.aulaia.dto.estudiante;

import java.time.OffsetDateTime;

/**
 * Response de un estudiante (contrato interno del Service; la API REST
 * llega en Prompt 4.3).
 *
 * <p>Incluye únicamente un resumen mínimo de la sección
 * ({@link SeccionResumen}: id y nombre); no se expone la entidad
 * {@code Seccion} completa. Nunca incluye datos sensibles; el
 * {@code qrToken} NO se expone en respuestas (token opaco de uso
 * interno/QR). Timestamps según el modelo oficial (04-BD §6.5).
 */
public record EstudianteResponse(
        Long id,
        String codigo,
        String nombres,
        String apellidos,
        SeccionResumen seccion,
        boolean activo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public record SeccionResumen(Long id, String nombre) {
    }
}