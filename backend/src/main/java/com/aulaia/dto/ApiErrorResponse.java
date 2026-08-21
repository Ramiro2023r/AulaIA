package com.aulaia.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Respuesta estándar de error de la API REST de AulaIA.
 *
 * Formato (docs/03-ARQUITECTURA_AulaIA.md):
 * <pre>
 * {
 *   "timestamp": "2026-08-17T18:00:00-05:00",
 *   "status": 400,
 *   "code": "VALIDATION_ERROR",
 *   "message": "Los datos enviados no son válidos"
 * }
 * </pre>
 *
 * Nunca expone stack traces ni detalles internos de PostgreSQL.
 * {@code path} y {@code details} se incluyen solo cuando aportan valor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> details) {

    public static ApiErrorResponse of(int status, String code, String message, String path) {
        return new ApiErrorResponse(OffsetDateTime.now(), status, code, message, path, null);
    }

    public static ApiErrorResponse of(int status, String code, String message, String path,
                                      Map<String, String> details) {
        return new ApiErrorResponse(OffsetDateTime.now(), status, code, message, path, details);
    }
}