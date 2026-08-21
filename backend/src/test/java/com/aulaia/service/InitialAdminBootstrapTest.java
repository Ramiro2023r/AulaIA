package com.aulaia.service;

import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link InitialAdminBootstrap} (Prompt 2.6), con
 * repositorio mockeado y BCrypt real. No dependen de PostgreSQL.
 */
class InitialAdminBootstrapTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private ApplicationArguments args;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        args = mock(ApplicationArguments.class);
    }

    private InitialAdminBootstrap bootstrap(boolean enabled, String username, String password) {
        return new InitialAdminBootstrap(usuarioRepository, passwordEncoder, enabled, username, password);
    }

    @Test
    void bootstrapDeshabilitadoNoCreaUsuario() {
        bootstrap(false, "admin", "cualquier-cosa-123456").run(args);

        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).existsByRol(any());
        verify(usuarioRepository, never()).existsByUsername(any());
    }

    @Test
    void bootstrapHabilitadoSinAdminCreaAdmin() {
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);
        when(usuarioRepository.existsByUsername("admin")).thenReturn(false);

        bootstrap(true, "admin", "segura-123456789").run(args);

        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void passwordSeGuardaConBcrypt() {
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);
        when(usuarioRepository.existsByUsername("admin")).thenReturn(false);
        String password = "segura-123456789";

        bootstrap(true, "admin", password).run(args);

        org.mockito.ArgumentCaptor<Usuario> captor =
                org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        String hash = captor.getValue().getPasswordHash();
        assertThat(hash).isNotEqualTo(password);
        assertThat(hash).startsWith("$2a$");
        assertThat(passwordEncoder.matches(password, hash)).isTrue();
    }

    @Test
    void adminCreadoTieneRolAdminActivoTrueYUltimoLoginAtNull() {
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);
        when(usuarioRepository.existsByUsername("admin")).thenReturn(false);

        bootstrap(true, "admin", "segura-123456789").run(args);

        org.mockito.ArgumentCaptor<Usuario> captor =
                org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario creado = captor.getValue();
        assertThat(creado.getUsername()).isEqualTo("admin");
        assertThat(creado.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.getUltimoLoginAt()).isNull();
    }

    @Test
    void siYaExisteAdminNoCreaOtro() {
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(true);

        bootstrap(true, "admin", "segura-123456789").run(args);

        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).existsByUsername(any());
    }

    @Test
    void ejecutarDosVecesDejaUnSoloAdmin() {
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false, true);
        when(usuarioRepository.existsByUsername("admin")).thenReturn(false);

        InitialAdminBootstrap bootstrap = bootstrap(true, "admin", "segura-123456789");
        bootstrap.run(args);
        bootstrap.run(args);

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    void passwordVaciaFalla() {
        assertThatThrownBy(() -> bootstrap(true, "admin", "").run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AULAIA_BOOTSTRAP_ADMIN_PASSWORD es obligatoria");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void passwordDemasiadoCortaFalla() {
        assertThatThrownBy(() -> bootstrap(true, "admin", "corta1234").run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("al menos 12 caracteres");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usernameVacioFalla() {
        assertThatThrownBy(() -> bootstrap(true, "   ", "segura-123456789").run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AULAIA_BOOTSTRAP_ADMIN_USERNAME es obligatorio");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usernameUsadoPorDocenteNoSeModificaYFalla() {
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);
        when(usuarioRepository.existsByUsername("docente")).thenReturn(true);

        assertThatThrownBy(() -> bootstrap(true, "docente", "segura-123456789").run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya existe con otro rol")
                .hasMessageContaining("no se modificará");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usernameMayorA100CaracteresFalla() {
        String largo = "a".repeat(101);
        assertThatThrownBy(() -> bootstrap(true, largo, "segura-123456789").run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no puede exceder 100 caracteres");
        verify(usuarioRepository, never()).save(any());
    }
}