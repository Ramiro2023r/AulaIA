package com.aulaia.service;

import com.aulaia.entity.Docente;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoUsersBootstrapTest {

    private UsuarioRepository usuarioRepository;
    private DocenteRepository docenteRepository;
    private PasswordEncoder passwordEncoder;
    private ApplicationArguments args;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        docenteRepository = mock(DocenteRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        args = mock(ApplicationArguments.class);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private DemoUsersBootstrap bootstrap(boolean enabled) {
        return new DemoUsersBootstrap(usuarioRepository, docenteRepository, passwordEncoder, enabled);
    }

    @Test
    void creaAdminSiNoExiste() {
        bootstrap(true).run(args);

        Usuario admin = usuariosGuardados().stream()
                .filter(usuario -> usuario.getUsername().equals(DemoUsersBootstrap.ADMIN_USERNAME))
                .findFirst().orElseThrow();
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.isActivo()).isTrue();
    }

    @Test
    void creaDocenteSiNoExisteConPerfilObligatorio() {
        bootstrap(true).run(args);

        Usuario docenteUsuario = usuariosGuardados().stream()
                .filter(usuario -> usuario.getUsername().equals(DemoUsersBootstrap.DOCENTE_USERNAME))
                .findFirst().orElseThrow();
        ArgumentCaptor<Docente> docenteCaptor = ArgumentCaptor.forClass(Docente.class);
        verify(docenteRepository).save(docenteCaptor.capture());
        Docente docente = docenteCaptor.getValue();

        assertThat(docenteUsuario.getRol()).isEqualTo(Rol.DOCENTE);
        assertThat(docenteUsuario.isActivo()).isTrue();
        assertThat(docente.getUsuario()).isSameAs(docenteUsuario);
        assertThat(docente.getNombres()).isEqualTo("Docente");
        assertThat(docente.getApellidos()).isEqualTo("Prueba");
        assertThat(docente.isActivo()).isTrue();
    }

    @Test
    void noDuplicaAdminSiYaExiste() {
        when(usuarioRepository.existsByUsername(DemoUsersBootstrap.ADMIN_USERNAME)).thenReturn(true);

        bootstrap(true).run(args);

        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.argThat(
                usuario -> DemoUsersBootstrap.ADMIN_USERNAME.equals(usuario.getUsername())));
    }

    @Test
    void noDuplicaDocenteSiYaExiste() {
        when(usuarioRepository.existsByUsername(DemoUsersBootstrap.DOCENTE_USERNAME)).thenReturn(true);

        bootstrap(true).run(args);

        verify(docenteRepository, never()).save(any());
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.argThat(
                usuario -> DemoUsersBootstrap.DOCENTE_USERNAME.equals(usuario.getUsername())));
    }

    @Test
    void passwordsSeGuardanCifradosConBcrypt() {
        bootstrap(true).run(args);

        Usuario admin = usuariosGuardados().stream()
                .filter(usuario -> usuario.getRol() == Rol.ADMIN).findFirst().orElseThrow();
        Usuario docente = usuariosGuardados().stream()
                .filter(usuario -> usuario.getRol() == Rol.DOCENTE).findFirst().orElseThrow();

        assertThat(admin.getPasswordHash()).isNotEqualTo(DemoUsersBootstrap.ADMIN_PASSWORD);
        assertThat(docente.getPasswordHash()).isNotEqualTo(DemoUsersBootstrap.DOCENTE_PASSWORD);
        assertThat(passwordEncoder.matches(DemoUsersBootstrap.ADMIN_PASSWORD, admin.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(DemoUsersBootstrap.DOCENTE_PASSWORD, docente.getPasswordHash())).isTrue();
    }

    @Test
    void deshabilitadoNoCreaUsuarios() {
        bootstrap(false).run(args);

        verify(usuarioRepository, never()).existsByUsername(any());
        verify(usuarioRepository, never()).save(any());
        verify(docenteRepository, never()).save(any());
    }

    @Test
    void seRegistraSoloEnPerfilDevYNoEnProduccion() {
        Profile profile = DemoUsersBootstrap.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("dev");
    }

    private List<Usuario> usuariosGuardados() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(2)).save(captor.capture());
        return captor.getAllValues();
    }
}
