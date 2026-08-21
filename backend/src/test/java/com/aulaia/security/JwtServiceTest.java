package com.aulaia.security;

import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de {@link JwtService} (Prompt 2.3), aisladas de
 * Spring Security y de la base de datos. Sin sleeps: la expiración se
 * prueba con duraciones controladas.
 */
class JwtServiceTest {

    private static final String TEST_SECRET = "test-only-secret-aulaia-unit-1234567890-abcdef";
    private static final long EXPIRATION_MS = 3600000;

    private JwtService jwtService;
    private Usuario profesor;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
        profesor = new Usuario();
        profesor.setId(1L);
        profesor.setUsername("profesor");
        profesor.setPasswordHash("$2a$10$no-es-parte-del-jwt");
        profesor.setRol(Rol.DOCENTE);
        profesor.setActivo(true);
    }

    @Test
    void generaTokenNoVacio() {
        assertThat(jwtService.generateToken(profesor)).isNotBlank();
    }

    @Test
    void extractUsernameDevuelveUsername() {
        String token = jwtService.generateToken(profesor);

        assertThat(jwtService.extractUsername(token)).isEqualTo("profesor");
    }

    @Test
    void extractUserIdDevuelveId() {
        String token = jwtService.generateToken(profesor);

        assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
    }

    @Test
    void extractRoleDevuelveRol() {
        String token = jwtService.generateToken(profesor);

        assertThat(jwtService.extractRole(token)).isEqualTo(Rol.DOCENTE);
    }

    @Test
    void tokenValidoParaUsuarioEsperado() {
        String token = jwtService.generateToken(profesor);

        assertThat(jwtService.isTokenValid(token, "profesor")).isTrue();
    }

    @Test
    void tokenInvalidoParaUsernameIncorrecto() {
        String token = jwtService.generateToken(profesor);

        assertThat(jwtService.isTokenValid(token, "otro-usuario")).isFalse();
    }

    @Test
    void firmaIncorrectaDevuelveInvalido() {
        JwtService otroServicio = new JwtService(
                "otro-secreto-de-prueba-diferente-1234567890-xyz", EXPIRATION_MS);
        String token = otroServicio.generateToken(profesor);

        assertThat(jwtService.isTokenValid(token, "profesor")).isFalse();
        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenExpiradoDevuelveExpiradoEInvalido() {
        String token = jwtService.generateToken(profesor, Duration.ofSeconds(-1));

        assertThat(jwtService.isTokenExpired(token)).isTrue();
        assertThat(jwtService.isTokenValid(token, "profesor")).isFalse();
    }

    @Test
    void tokenMalformadoDevuelveInvalidoSinExplosion() {
        String malformado = "abc.xyz";

        assertThat(jwtService.isTokenValid(malformado, "profesor")).isFalse();
        assertThat(jwtService.isTokenExpired(malformado)).isFalse();
        assertThatThrownBy(() -> jwtService.extractUsername(malformado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenVacioDevuelveInvalido() {
        assertThat(jwtService.isTokenValid("", "profesor")).isFalse();
        assertThat(jwtService.isTokenExpired("")).isFalse();
    }

    @Test
    void secretoCortoFallaAlConstruir() {
        assertThatThrownBy(() -> new JwtService("secreto-corto", EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void secretoVacioFallaAlConstruir() {
        assertThatThrownBy(() -> new JwtService("", EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class);
    }
}