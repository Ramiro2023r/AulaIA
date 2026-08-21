package com.aulaia.security;

import com.aulaia.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Respuesta 403 consistente con {@link ApiErrorResponse} cuando un usuario
 * autenticado no tiene permisos suficientes para la operación (roles).
 *
 * <p>Formato: {@code {"status":403,"code":"FORBIDDEN","message":"No tienes permiso...",...}}
 * Nunca HTML ni stack traces.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiErrorResponse.of(HttpStatus.FORBIDDEN.value(), "FORBIDDEN",
                        "No tienes permiso para realizar esta acción", request.getRequestURI()));
    }
}