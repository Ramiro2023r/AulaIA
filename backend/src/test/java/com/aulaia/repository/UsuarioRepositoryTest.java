package com.aulaia.repository;

import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración de {@link UsuarioRepository} contra PostgreSQL
 * real (perfil {@code itest}).
 *
 * <p>Deshabilitadas por defecto para no depender de una instalación
 * PostgreSQL personal. Ejecución explícita (PowerShell):
 * <pre>
 * $env:DB_HOST="localhost"; $env:DB_PORT="5433"; $env:DB_NAME="aulaia_db"
 * $env:DB_USERNAME="aulaia_user"; $env:DB_PASSWORD="TU_PASSWORD_LOCAL"
 * .\mvnw.cmd test -Dtest=UsuarioRepositoryTest -Daulaia.itest=true
 * </pre>
 *
 * <p>Las pruebas son transaccionales: cada test revierte sus cambios,
 * la base de datos local no se contamina.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("itest")
@EnabledIfSystemProperty(named = "aulaia.itest", matches = "true")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    @Test
    void guardarUsuario() {
        Usuario guardado = usuarioRepository.saveAndFlush(usuario("jperez", Rol.DOCENTE));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getUsername()).isEqualTo("jperez");
        assertThat(guardado.getRol()).isEqualTo(Rol.DOCENTE);
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getCreatedAt()).isNotNull();
        assertThat(guardado.getUpdatedAt()).isNotNull();
        assertThat(guardado.getUltimoLoginAt()).isNull();
    }

    @Test
    void buscarPorUsername() {
        usuarioRepository.saveAndFlush(usuario("lramirez", Rol.ADMIN));

        Optional<Usuario> encontrado = usuarioRepository.findByUsername("lramirez");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getRol()).isEqualTo(Rol.ADMIN);
        assertThat(encontrado.get().getPasswordHash()).isNotBlank();
    }

    @Test
    void usernameInexistente() {
        assertThat(usuarioRepository.findByUsername("no-existe")).isEmpty();
    }

    @Test
    void existsByUsername() {
        usuarioRepository.saveAndFlush(usuario("mfernandez", Rol.DOCENTE));

        assertThat(usuarioRepository.existsByUsername("mfernandez")).isTrue();
        assertThat(usuarioRepository.existsByUsername("otro-usuario")).isFalse();
    }

    @Test
    void usernameNoDuplicable() {
        usuarioRepository.saveAndFlush(usuario("duplicado", Rol.DOCENTE));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(usuario("duplicado", Rol.ADMIN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Usuario usuario(String username, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash("$2a$10$hash-de-ejemplo-para-tests-no-real");
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }
}