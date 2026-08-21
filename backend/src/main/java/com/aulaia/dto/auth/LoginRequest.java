package com.aulaia.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request de login (POST /api/v1/auth/login).
 *
 * <p>Nunca se registra en logs (especialmente {@code password}).
 */
public record LoginRequest(
        @NotBlank(message = "username es obligatorio")
        @Size(max = 100, message = "username no puede exceder 100 caracteres")
        String username,

        @NotBlank(message = "password es obligatorio")
        String password) {
}