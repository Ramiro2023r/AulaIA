package com.aulaia.dto.docente;

import java.time.OffsetDateTime;

/**
 * Respuesta de un docente (Prompt 5.1).
 *
 * <p>Sin información sensible: nunca se expone {@code password} ni
 * {@code passwordHash} (04-BD §6.1: la contraseña se almacena solo como
 * hash; 02-TRD: nunca en texto plano). El {@link UsuarioResumen} solo
 * incluye datos de identificación y estado de la cuenta asociada 1 a 1.
 */
public record DocenteResponse(
        Long id,
        String nombres,
        String apellidos,
        boolean activo,
        UsuarioResumen usuario,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public record UsuarioResumen(Long id, String username, String rol, boolean activo) {
    }
}