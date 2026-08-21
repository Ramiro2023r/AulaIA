package com.aulaia.dto.auth;

/**
 * Respuesta de login exitoso.
 *
 * <p>{@code expiresIn} está en SEGUNDOS (el JWT internamente usa
 * milisegundos). Nunca incluye passwordHash ni datos sensibles.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthenticatedUserResponse user) {

    public static LoginResponse of(String accessToken, long expiresInSeconds,
                                   AuthenticatedUserResponse user) {
        return new LoginResponse(accessToken, "Bearer", expiresInSeconds, user);
    }
}