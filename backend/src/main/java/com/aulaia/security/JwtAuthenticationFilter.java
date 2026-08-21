package com.aulaia.security;

import com.aulaia.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticación Bearer JWT (Prompt 2.4).
 *
 * <p>Flujo:
 * <ol>
 *   <li>Si ya existe autenticación válida en el contexto, no reprocesa.</li>
 *   <li>Lee {@code Authorization: Bearer <token>} (única fuente aceptada:
 *       ni query params, ni cookies, ni body).</li>
 *   <li>Sin header Bearer → continúa la cadena sin autenticar (Spring
 *       Security resuelve luego si la ruta requiere auth).</li>
 *   <li>Extrae username con {@link JwtService} y carga el usuario con
 *       {@link UserDetailsService} (rechaza inexistentes e inactivos).</li>
 *   <li>Valida el token (firma + expiración + username esperado).</li>
 *   <li>Si es válido, establece {@link UsernamePasswordAuthenticationToken}
 *       en el {@link SecurityContextHolder}.</li>
 * </ol>
 *
 * <p>Token inválido (expirado, firma inválida, malformado, vacío, claims
 * inválidos, usuario inexistente o inactivo) → <b>401</b> con
 * {@link ApiErrorResponse} ("Token inválido o expirado"), sin exponer la
 * razón criptográfica al cliente ni producir 500. Nunca se loguea el token
 * completo ni el header Authorization.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INVALID_TOKEN_MESSAGE = "Token inválido o expirado";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                   ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.isTokenValid(token, userDetails.getUsername())) {
                sendUnauthorized(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException ex) {
            log.warn("Autenticación JWT rechazada en {}: {}",
                    request.getRequestURI(), ex.getClass().getSimpleName());
            sendUnauthorized(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                        INVALID_TOKEN_MESSAGE, request.getRequestURI()));
    }
}