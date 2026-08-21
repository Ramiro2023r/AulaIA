package com.aulaia.repository;

import com.aulaia.dto.docente.DocenteRequest;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.service.DocenteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Pruebas de integración de {@link DocenteRepository} y del módulo Docentes
 * contra PostgreSQL real (perfil {@code itest}), siguiendo el patrón de
 * {@code EstudianteRepositoryTest}.
 *
 * <p>Deshabilitadas por defecto. Ejecución explícita (PowerShell):
 * <pre>
 * $env:DB_HOST="localhost"; $env:DB_PORT="5433"; $env:DB_NAME="aulaia_db"
 * $env:DB_USERNAME="aulaia_user"; $env:DB_PASSWORD="TU_PASSWORD_LOCAL"
 * .\mvnw.cmd test "-Dtest=DocenteRepositoryTest" "-Daulaia.itest=true"
 * </pre>
 *
 * <p>Transaccionales: cada test revierte sus cambios. Verifican las
 * restricciones físicas de {@code docentes} (04-BD §6.2): FK a usuarios,
 * UNIQUE de usuario_id (1:1) y UNIQUE de username. Datos ficticios.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("itest")
@EnabledIfSystemProperty(named = "aulaia.itest", matches = "true")
class DocenteRepositoryTest {

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DocenteService docenteService;

    @MockitoSpyBean
    private DocenteRepository docenteRepositorySpy;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    private Usuario guardarUsuario(String username) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash("$2a$10$hashFicticioParaPruebasSoloLocal1234567890");
        usuario.setRol(Rol.DOCENTE);
        usuario.setActivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Docente docente(Usuario usuario, String nombres, String apellidos) {
        Docente docente = new Docente();
        docente.setUsuario(usuario);
        docente.setNombres(nombres);
        docente.setApellidos(apellidos);
        return docente;
    }

    @Test
    void guardarDocenteCorrectamente() {
        Usuario usuario = guardarUsuario("doc.uno");

        Docente guardado = docenteRepository.saveAndFlush(docente(usuario, "Docente Uno", "Apellido Uno"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getNombres()).isEqualTo("Docente Uno");
        assertThat(guardado.getApellidos()).isEqualTo("Apellido Uno");
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getUsuario().getId()).isEqualTo(usuario.getId());
        assertThat(guardado.getUsuario().getUsername()).isEqualTo("doc.uno");
        assertThat(guardado.getUsuario().getRol()).isEqualTo(Rol.DOCENTE);
        assertThat(guardado.getCreatedAt()).isNotNull();
        assertThat(guardado.getUpdatedAt()).isNotNull();
    }

    @Test
    void buscarDocentePorId() {
        Usuario usuario = guardarUsuario("doc.dos");
        Docente guardado = docenteRepository.saveAndFlush(docente(usuario, "Docente Dos", "Apellido Dos"));

        Optional<Docente> encontrado = docenteRepository.findById(guardado.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsuario().getUsername()).isEqualTo("doc.dos");
    }

    @Test
    void buscarDocentePorUsuarioId() {
        Usuario usuario = guardarUsuario("doc.tres");
        docenteRepository.saveAndFlush(docente(usuario, "Docente Tres", "Apellido Tres"));

        Optional<Docente> encontrado = docenteRepository.findByUsuarioId(usuario.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNombres()).isEqualTo("Docente Tres");
    }

    @Test
    void existsByUsuarioId() {
        Usuario usuario = guardarUsuario("doc.cuatro");
        docenteRepository.saveAndFlush(docente(usuario, "Docente Cuatro", "Apellido Cuatro"));

        assertThat(docenteRepository.existsByUsuarioId(usuario.getId())).isTrue();
        assertThat(docenteRepository.existsByUsuarioId(999999L)).isFalse();
    }

    @Test
    void unSoloPerfilDocentePorUsuario() {
        Usuario usuario = guardarUsuario("doc.cinco");
        docenteRepository.saveAndFlush(docente(usuario, "Docente Cinco", "Apellido Cinco"));

        Docente segundo = docente(usuario, "Segundo Perfil", "Invalido");

        assertThatThrownBy(() -> docenteRepository.saveAndFlush(segundo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void usernameDuplicadoEsRechazado() {
        guardarUsuario("doc.seis");

        assertThatThrownBy(() -> guardarUsuario("doc.seis"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listarDocentesOrdenadosPorId() {
        Usuario usuarioA = guardarUsuario("doc.siete");
        Usuario usuarioB = guardarUsuario("doc.ocho");
        docenteRepository.saveAndFlush(docente(usuarioA, "Docente Siete", "Apellido Siete"));
        docenteRepository.saveAndFlush(docente(usuarioB, "Docente Ocho", "Apellido Ocho"));

        List<Docente> lista = docenteRepository.findAllByOrderByIdAsc();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getId()).isLessThan(lista.get(1).getId());
    }

    /**
     * Prueba real de transaccionalidad (Prompt 5.1 §19): si falla la
     * persistencia del Docente, la creación de la cuenta DOCENTE se
     * revierte y no queda un usuario huérfano. Corre SIN transacción propia
     * (NOT_SUPPORTED) para que el rollback del Service sea real; el
     * repository se espía para forzar el fallo tras el guardado del usuario.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void falloAlPersistirDocenteNoDejaUsuarioHuerfano() {
        String username = "doc.transaccional";
        try {
            doThrow(new RuntimeException("fallo simulado al persistir el docente"))
                    .when(docenteRepositorySpy).saveAndFlush(any(Docente.class));

            assertThatThrownBy(() -> docenteService.crear(
                    new DocenteRequest(username, "clave-ficticia-123", "Docente", "Transaccional")))
                    .isInstanceOf(RuntimeException.class);

            Optional<Usuario> huerfano = usuarioRepository.findByUsername(username);
            assertThat(huerfano).isEmpty();
        } finally {
            usuarioRepository.findByUsername(username).ifPresent(usuarioRepository::delete);
        }
    }
}