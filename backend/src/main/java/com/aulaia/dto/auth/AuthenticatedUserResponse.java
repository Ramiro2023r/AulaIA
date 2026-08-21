package com.aulaia.dto.auth;

import com.aulaia.entity.Rol;

/**
 * Usuario autenticado (respuesta de login). Solo lo necesario para el
 * frontend: sin passwordHash, sin activo, sin datos sensibles.
 */
public record AuthenticatedUserResponse(Long id, String username, Rol rol) {
}