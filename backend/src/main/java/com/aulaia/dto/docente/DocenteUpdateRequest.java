package com.aulaia.dto.docente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para actualizar un docente (Prompt 5.1).
 *
 * <p>Contrato mínimo y sin ambigüedad: solo actualiza los datos académicos
 * autorizados (nombres/apellidos). No cambia usuario, rol ni contraseña:
 * los documentos no definen cambio de credenciales desde este módulo
 * (07-PLAN 5.1 no lo exige). {@code activo} tampoco: la desactivación es la
 * operación documentada (06-FLUJOS #49).
 */
public record DocenteUpdateRequest(
        @NotBlank(message = "nombres es obligatorio")
        @Size(max = 120, message = "nombres no puede exceder 120 caracteres")
        String nombres,

        @NotBlank(message = "apellidos es obligatorio")
        @Size(max = 120, message = "apellidos no puede exceder 120 caracteres")
        String apellidos) {
}