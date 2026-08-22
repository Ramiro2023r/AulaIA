package com.aulaia.service;

import com.aulaia.dto.docente.DocenteRequest;
import com.aulaia.dto.docente.DocenteResponse;
import com.aulaia.dto.docente.DocenteUpdateRequest;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.DocenteMapperImpl;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link DocenteService} (Prompt 5.1), con
 * repositorios mockeados, {@link DocenteMapperImpl} real y
 * {@link BCryptPasswordEncoder} real (el mismo del proyecto). No dependen
 * de PostgreSQL. Datos ficticios, nunca de menores reales.
 */
class DocenteServiceTest {

    private DocenteRepository docenteRepository;
    private UsuarioRepository usuarioRepository;
    private DocenteService docenteService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        docenteRepository = mock(DocenteRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        docenteService = new DocenteService(docenteRepository, usuarioRepository, new DocenteMapperImpl(), passwordEncoder);
    }

    private Usuario usuario(Long id, String username, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setPasswordHash("hash-irrelevante-en-mock");
        usuario.setRol(Rol.DOCENTE);
        usuario.setActivo(activo);
        return usuario;
    }

    private Docente docente(Long id, Usuario usuario, String nombres, String apellidos, boolean activo) {
        Docente docente = new Docente();
        docente.setId(id);
        docente.setUsuario(usuario);
        docente.setNombres(nombres);
        docente.setApellidos(apellidos);
        docente.setActivo(activo);
        return docente;
    }

    private DocenteRequest request(String username, String password) {
        return new DocenteRequest(username, password, "Docente", "De Prueba");
    }

    private void stubCrearExitoso() {
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(docenteRepository.saveAndFlush(any(Docente.class))).thenAnswer(inv -> {
            Docente d = inv.getArgument(0);
            d.setId(10L);
            return d;
        });
    }

    @Test
    void crearDocenteCreaUsuarioConRolDocenteYPasswordHasheada() {
        stubCrearExitoso();

        DocenteResponse response = docenteService.crear(request("d.profesor", "clave-ficticia-123"));

        ArgumentCaptor<Usuario> captorUsuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captorUsuario.capture());
        Usuario usuarioGuardado = captorUsuario.getValue();
        assertThat(usuarioGuardado.getUsername()).isEqualTo("d.profesor");
        assertThat(usuarioGuardado.getRol()).isEqualTo(Rol.DOCENTE);
        assertThat(usuarioGuardado.isActivo()).isTrue();
        assertThat(usuarioGuardado.getPasswordHash()).isNotEqualTo("clave-ficticia-123");
        assertThat(passwordEncoder.matches("clave-ficticia-123", usuarioGuardado.getPasswordHash())).isTrue();

        ArgumentCaptor<Docente> captorDocente = ArgumentCaptor.forClass(Docente.class);
        verify(docenteRepository).saveAndFlush(captorDocente.capture());
        Docente docenteGuardado = captorDocente.getValue();
        assertThat(docenteGuardado.getNombres()).isEqualTo("Docente");
        assertThat(docenteGuardado.getApellidos()).isEqualTo("De Prueba");
        assertThat(docenteGuardado.getUsuario().getId()).isEqualTo(1L);
        assertThat(docenteGuardado.isActivo()).isTrue();

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.usuario().username()).isEqualTo("d.profesor");
        assertThat(response.usuario().rol()).isEqualTo("DOCENTE");
    }

    @Test
    void passwordNoQuedaEnTextoPlanoNiSeExponeEnRespuesta() {
        stubCrearExitoso();

        DocenteResponse response = docenteService.crear(request("d.profesor", "clave-ficticia-123"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("clave-ficticia-123");
        assertThat(captor.getValue().getPasswordHash()).doesNotContain("clave-ficticia");
        assertThat(response.toString()).doesNotContain("clave-ficticia-123");
        assertThat(response.usuario().toString()).doesNotContain("clave-ficticia-123");
    }

    @Test
    void crearDocenteConUsernameExistenteLanzaConflictUsernameAlreadyExists() {
        when(usuarioRepository.existsByUsername("d.profesor")).thenReturn(true);

        assertThatThrownBy(() -> docenteService.crear(request("d.profesor", "clave-ficticia-123")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("username 'd.profesor'")
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    void crearDocenteNormalizaUsername() {
        stubCrearExitoso();

        docenteService.crear(request("  d.profesor  ", "clave-ficticia-123"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("d.profesor");
    }

    @Test
    void buscarDocenteExistente() {
        Docente docente = docente(10L, usuario(1L, "d.profesor", true), "Docente", "De Prueba", true);
        when(docenteRepository.findById(10L)).thenReturn(Optional.of(docente));

        DocenteResponse response = docenteService.buscarPorId(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.nombres()).isEqualTo("Docente");
        assertThat(response.usuario().username()).isEqualTo("d.profesor");
    }

    @Test
    void buscarDocenteInexistenteLanzaTeacherNotFound() {
        when(docenteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> docenteService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Docente no encontrado")
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode()).isEqualTo("TEACHER_NOT_FOUND"));
    }

    @Test
    void listarDocentesOrdenadosPorId() {
        when(docenteRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(docente(1L, usuario(1L, "d.uno", true), "Uno", "Docente", true)));

        List<DocenteResponse> lista = docenteService.listar();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).id()).isEqualTo(1L);
    }

    @Test
    void actualizarDocenteSoloCambiaNombresYApellidos() {
        Usuario usuario = usuario(1L, "d.profesor", true);
        Docente docente = docente(10L, usuario, "Docente", "De Prueba", true);
        when(docenteRepository.findById(10L)).thenReturn(Optional.of(docente));
        when(docenteRepository.saveAndFlush(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

        DocenteResponse response = docenteService.actualizar(10L, new DocenteUpdateRequest("Docente Actualizado", "Nuevo Apellido"));

        assertThat(response.nombres()).isEqualTo("Docente Actualizado");
        assertThat(response.apellidos()).isEqualTo("Nuevo Apellido");
        assertThat(response.usuario().username()).isEqualTo("d.profesor");
        assertThat(docente.getUsuario().getPasswordHash()).isEqualTo("hash-irrelevante-en-mock");
    }

    @Test
    void actualizarDocenteInexistenteLanzaTeacherNotFound() {
        when(docenteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> docenteService.actualizar(99L, new DocenteUpdateRequest("Docente", "De Prueba")))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode()).isEqualTo("TEACHER_NOT_FOUND"));
    }

    @Test
    void desactivarDocentePoneActivoFalseEnDocenteYUsuario() {
        Usuario usuario = usuario(1L, "d.profesor", true);
        Docente docente = docente(10L, usuario, "Docente", "De Prueba", true);
        when(docenteRepository.findById(10L)).thenReturn(Optional.of(docente));
        when(docenteRepository.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(docenteRepository.saveAndFlush(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

        DocenteResponse response = docenteService.desactivar(10L);

        assertThat(docente.isActivo()).isFalse();
        assertThat(usuario.isActivo()).isFalse();
        assertThat(response.activo()).isFalse();
        assertThat(response.usuario().activo()).isFalse();
        verify(usuarioRepository).saveAndFlush(usuario);
    }

    @Test
    void desactivarDocenteInexistenteLanzaTeacherNotFound() {
        when(docenteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> docenteService.desactivar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode()).isEqualTo("TEACHER_NOT_FOUND"));
    }
}