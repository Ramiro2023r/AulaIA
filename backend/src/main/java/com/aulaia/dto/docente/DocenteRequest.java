package com.aulaia.dto.docente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para crear un docente (Prompt 5.1, 07-PLAN).
 *
 * <p>La creación crea también el {@code Usuario} (06-FLUJOS #9: "Crear
 * usuario → Asignar rol DOCENTE → Crear perfil docente"): el request recibe
 * las credenciales de forma explícita (username/password). No se genera
 * username automáticamente (no documentado) y la contraseña nunca se
 * almacena en texto plano (BCrypt en el Service).
 *
 * <p>{@code activo} NO forma parte del contrato: el modelo oficial
 * (04-BD §6.2) define {@code activo BOOLEAN NOT NULL DEFAULT TRUE} y la
 * desactivación tiene su propia operación documentada (06-FLUJOS #49).
 *
 * <p>Longitudes según 04-BD: username VARCHAR(100), nombres y apellidos
 * VARCHAR(120), password_hash VARCHAR(255) (límite de BCrypt ≪ 255).
 */
public record DocenteRequest(
        @NotBlank(message = "username es obligatorio")
        @Size(max = 100, message = "username no puede exceder 100 caracteres")
        @Schema(description = "Username de la cuenta DOCENTE a crear (único)")
        String username,

        @NotBlank(message = "password es obligatoria")
        @Schema(description = "Contraseña de la cuenta DOCENTE a crear", accessMode = Schema.AccessMode.WRITE_ONLY)
        String password,

        @NotBlank(message = "nombres es obligatorio")
        @Size(max = 120, message = "nombres no puede exceder 120 caracteres")
        String nombres,

        @NotBlank(message = "apellidos es obligatorio")
        @Size(max = 120, message = "apellidos no puede exceder 120 caracteres")
        String apellidos) {
}