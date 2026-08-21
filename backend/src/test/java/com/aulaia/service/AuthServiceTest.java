package com.aulaia.service;

import com.aulaia.dto.auth.LoginRequest;
import com.aulaia.dto.auth.LoginResponse;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.InvalidCredentialsException;
import com.aulaia.repository.UsuarioRepository;
import com.aulaia.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link AuthService} (Prompt 2.5), con repositorio
 * mockeado y BCrypt/JwtService reales. Sin PostgreSQL.
 */
class AuthServiceTest {

    private static final String TEST_SECRET = "test-only-secret-aulaia-unit-1234567890-abcdef";
    private static final long EXPIRATION_MS = 3600000;

    private UsuarioRepository usuarioRepository;
    private AuthService authService;
    private JwtService jwtService;

    private Usuario admin;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtService, EXPIRATION_MS);

        admin = new Usuario();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("adminpass123"));
        admin.setRol(Rol.ADMIN);
        admin.setActivo(true);
    }

    @Test
    void loginCorrectoGeneraJwtConRolAdminYActualizaUltimoLoginAt() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        LoginResponse response = authService.login(new LoginRequest("admin", "adminpass123"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(EXPIRATION_MS / 1000);
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().rol()).isEqualTo(Rol.ADMIN);
        assertThat(jwtService.extractUsername(response.accessToken())).isEqualTo("admin");
        assertThat(jwtService.extractRole(response.accessToken())).isEqualTo(Rol.ADMIN);
        assertThat(admin.getUltimoLoginAt()).isNotNull();
        verify(usuarioRepository).save(admin);
    }

    @Test
    void loginDocenteDevuelveRolDocente() {
        Usuario docente = new Usuario();
        docente.setId(2L);
        docente.setUsername("docente");
        docente.setPasswordHash(new BCryptPasswordEncoder().encode("docente123"));
        docente.setRol(Rol.DOCENTE);
        docente.setActivo(true);
        when(usuarioRepository.findByUsername("docente")).thenReturn(Optional.of(docente));

        LoginResponse response = authService.login(new LoginRequest("docente", "docente123"));

        assertThat(response.user().rol()).isEqualTo(Rol.DOCENTE);
        assertThat(jwtService.extractRole(response.accessToken())).isEqualTo(Rol.DOCENTE);
    }

    @Test
    void passwordIncorrectaLanzaInvalidCredentials() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "password-mala")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(InvalidCredentialsException.MESSAGE);
        assertThat(admin.getUltimoLoginAt()).isNull();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usernameInexistentelanzaInvalidCredentials() {
        when(usuarioRepository.findByUsername("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("no-existe", "cualquiera")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(InvalidCredentialsException.MESSAGE);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usuarioInactivoLanzaInvalidCredentials() {
        admin.setActivo(false);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "adminpass123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(InvalidCredentialsException.MESSAGE);
        assertThat(admin.getUltimoLoginAt()).isNull();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void ultimoLoginAtNoSeActualizaEnLoginFallido() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "incorrecta")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(admin.getUltimoLoginAt()).isNull();
        verify(usuarioRepository, never()).save(any());
    }
}