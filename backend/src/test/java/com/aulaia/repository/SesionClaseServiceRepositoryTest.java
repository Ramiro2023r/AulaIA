package com.aulaia.repository;

import com.aulaia.dto.sesion.SesionClaseResponse;
import com.aulaia.entity.Curso;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Grado;
import com.aulaia.entity.Horario;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.SesionClase;
import com.aulaia.entity.SesionClaseEstado;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ForbiddenOperationException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.service.SesionClaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Pruebas de integración de {@link SesionClaseService#obtenerOCrearSesion}
 * contra PostgreSQL real (perfil {@code itest}), Prompt 6.2.
 *
 * <p>Deshabilitadas por defecto. Ejecución explícita (PowerShell):
 * <pre>
 * $env:DB_HOST="localhost"; $env:DB_PORT="5433"; $env:DB_NAME="aulaia_db"
 * $env:DB_USERNAME="aulaia_user"; $env:DB_PASSWORD="TU_PASSWORD_LOCAL"
 * .\mvnw.cmd test "-Dtest=SesionClaseServiceRepositoryTest" "-Daulaia.itest=true"
 * </pre>
 *
 * <p>NO transaccional a propósito: la creación de la sesión ocurre en una
 * transacción REQUIRES_NEW ({@code SesionClaseCreatorTx}), que solo ve
 * datos commiteados. Los horarios se crean con commits propios y cada test
 * limpia sus datos en {@link #tearDown()}. Datos ficticios con prefijos
 * {@code * SVC} / {@code doc.sesion.svc%}, nunca reales.
 */
@SpringBootTest
@ActiveProfiles("itest")
@EnabledIfSystemProperty(named = "aulaia.itest", matches = "true")
class SesionClaseServiceRepositoryTest {

    @Autowired
    private SesionClaseService sesionClaseService;

    @Autowired
    private SesionClaseRepository sesionClaseRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private GradoRepository gradoRepository;

    @Autowired
    private SeccionRepository seccionRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static int contadorUsuarios;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("""
                DELETE FROM sesiones_clase WHERE horario_id IN (
                    SELECT h.id FROM horarios h
                    JOIN docentes d ON d.id = h.docente_id
                    JOIN usuarios u ON u.id = d.usuario_id
                    WHERE u.username LIKE 'doc.sesion.svc%')""");
        jdbcTemplate.update("""
                DELETE FROM horarios WHERE docente_id IN (
                    SELECT d.id FROM docentes d
                    JOIN usuarios u ON u.id = d.usuario_id
                    WHERE u.username LIKE 'doc.sesion.svc%')""");
        jdbcTemplate.update("""
                DELETE FROM docentes WHERE usuario_id IN (
                    SELECT id FROM usuarios WHERE username LIKE 'doc.sesion.svc%')""");
        jdbcTemplate.update("DELETE FROM usuarios WHERE username LIKE 'doc.sesion.svc%'");
        jdbcTemplate.update("""
                DELETE FROM secciones WHERE grado_id IN (
                    SELECT id FROM grados WHERE nombre = '6.º Primaria SVC')""");
        jdbcTemplate.update("DELETE FROM cursos WHERE nombre = 'Matemática SVC'");
        jdbcTemplate.update("DELETE FROM grados WHERE nombre = '6.º Primaria SVC'");
    }

    private Horario guardarHorario() {
        return guardarHorario("doc.sesion.svc" + (++contadorUsuarios));
    }

    private Horario guardarHorario(String username) {
        Curso curso = new Curso();
        curso.setNombre("Matemática SVC");
        curso = cursoRepository.saveAndFlush(curso);

        Grado grado = new Grado();
        grado.setNombre("6.º Primaria SVC");
        grado = gradoRepository.saveAndFlush(grado);
        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setNombre("A");
        seccion.setPeriodoAcademico("2026");
        seccion = seccionRepository.saveAndFlush(seccion);

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash("$2a$10$hashFicticioParaPruebasSoloLocal1234567890");
        usuario.setRol(Rol.DOCENTE);
        usuario.setActivo(true);
        usuario = usuarioRepository.saveAndFlush(usuario);
        Docente docente = new Docente();
        docente.setUsuario(usuario);
        docente.setNombres("Docente");
        docente.setApellidos("De Prueba");
        docente = docenteRepository.saveAndFlush(docente);

        Horario horario = new Horario();
        horario.setCurso(curso);
        horario.setSeccion(seccion);
        horario.setDocente(docente);
        horario.setDiaSemana((short) 1);
        horario.setHoraInicio(LocalTime.of(9, 0));
        horario.setHoraFin(LocalTime.of(10, 30));
        return horarioRepository.saveAndFlush(horario);
    }

    private void autenticar(String username, Rol rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new User(username, "x",
                                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))),
                        "x", List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))));
    }

    @Test
    void primeraLlamadaCreaYSegundaDevuelveLaMisma() {
        Horario horario = guardarHorario();
        LocalDate fecha = LocalDate.of(2026, 8, 19);

        SesionClase s1 = sesionClaseService.obtenerOCrearSesion(horario.getId(), fecha);
        SesionClase s2 = sesionClaseService.obtenerOCrearSesion(horario.getId(), fecha);

        assertThat(s1.getId()).isNotNull();
        assertThat(s1.getEstado()).isEqualTo(SesionClaseEstado.PROGRAMADA);
        assertThat(s1.getHoraApertura()).isNull();
        assertThat(s1.getHoraCierre()).isNull();
        assertThat(s2.getId()).isEqualTo(s1.getId());

        Number count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sesiones_clase WHERE horario_id = ? AND fecha = ?""",
                Number.class, horario.getId(), fecha);
        assertThat(count.longValue()).isEqualTo(1L);
    }

    @Test
    void mismoHorarioOtraFechaPermiteOtraSesion() {
        Horario horario = guardarHorario();

        SesionClase dia19 = sesionClaseService.obtenerOCrearSesion(horario.getId(), LocalDate.of(2026, 8, 19));
        SesionClase dia20 = sesionClaseService.obtenerOCrearSesion(horario.getId(), LocalDate.of(2026, 8, 20));

        assertThat(dia20.getId()).isNotEqualTo(dia19.getId());
        assertThat(dia20.getFecha()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void otroHorarioMismaFechaPermiteOtraSesion() {
        Horario horarioA = guardarHorario();
        Horario horarioB = guardarHorario();
        LocalDate fecha = LocalDate.of(2026, 8, 19);

        SesionClase delA = sesionClaseService.obtenerOCrearSesion(horarioA.getId(), fecha);
        SesionClase delB = sesionClaseService.obtenerOCrearSesion(horarioB.getId(), fecha);

        assertThat(delB.getId()).isNotEqualTo(delA.getId());
        assertThat(delB.getHorario().getId()).isEqualTo(horarioB.getId());
    }

    @Test
    void carreraDeUniqueSeRecuperaDevolviendoLaSesionPersistida() {
        Horario horario = guardarHorario();
        LocalDate fecha = LocalDate.of(2026, 8, 27);
        jdbcTemplate.update("""
                INSERT INTO sesiones_clase (horario_id, fecha)
                VALUES (?, ?)""", horario.getId(), fecha);

        SesionClase resultado = sesionClaseService.obtenerOCrearSesion(horario.getId(), fecha);

        assertThat(resultado.getFecha()).isEqualTo(fecha);
        assertThat(resultado.getEstado()).isEqualTo(SesionClaseEstado.PROGRAMADA);
        Number count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sesiones_clase WHERE horario_id = ? AND fecha = ?""",
                Number.class, horario.getId(), fecha);
        assertThat(count.longValue()).isEqualTo(1L);
    }

    @Test
    void horarioInexistenteLanzaScheduleNotFound() {
        assertThat(sesionClaseRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        sesionClaseService.obtenerOCrearSesion(999999L, LocalDate.of(2026, 8, 19)))
                .isInstanceOf(com.aulaia.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Horario no encontrado");
    }

    // ===================== Prompt 6.3 — apertura (PostgreSQL real) =====================

    @Test
    void abrirProgramadaEnBdQuedaAbiertaConHoraYsinCierre() {
        autenticar("admin.svc", Rol.ADMIN);
        Horario horario = guardarHorario("doc.sesion.svc.a");
        SesionClase sesion = sesionClaseService.obtenerOCrearSesion(horario.getId(), LocalDate.of(2026, 8, 19));
        assertThat(sesion.getEstado()).isEqualTo(SesionClaseEstado.PROGRAMADA);

        var respuesta = sesionClaseService.abrirSesion(sesion.getId());

        assertThat(respuesta.estado()).isEqualTo(SesionClaseEstado.ABIERTA);
        assertThat(respuesta.horaApertura()).isNotNull();
        assertThat(respuesta.horaCierre()).isNull();
        String fila = jdbcTemplate.queryForObject("""
                SELECT estado || '|' || (hora_apertura IS NOT NULL) || '|' || (hora_cierre IS NULL)
                FROM sesiones_clase WHERE id = ?""", String.class, sesion.getId());
        assertThat(fila).isEqualTo("ABIERTA|true|true");
    }

    @Test
    void docentePropietarioAbreSuSesion() {
        autenticar("doc.sesion.svc.a", Rol.DOCENTE);
        Horario horario = guardarHorario("doc.sesion.svc.a");
        SesionClase sesion = sesionClaseService.obtenerOCrearSesion(horario.getId(), LocalDate.of(2026, 8, 19));

        var respuesta = sesionClaseService.abrirSesion(sesion.getId());

        assertThat(respuesta.estado()).isEqualTo(SesionClaseEstado.ABIERTA);
        assertThat(respuesta.horaApertura()).isNotNull();
    }

    @Test
    void docenteAjenoNoAbreYLaSesionSigueProgramada() {
        Horario horarioA = guardarHorario("doc.sesion.svc.a");
        Horario horarioB = guardarHorario("doc.sesion.svc.b");
        SesionClase sesionB = sesionClaseService.obtenerOCrearSesion(horarioB.getId(), LocalDate.of(2026, 8, 19));
        autenticar("doc.sesion.svc.a", Rol.DOCENTE);

        assertThatThrownBy(() -> sesionClaseService.abrirSesion(sesionB.getId()))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("otro docente");

        String fila = jdbcTemplate.queryForObject("""
                SELECT estado || '|' || (hora_apertura IS NULL)
                FROM sesiones_clase WHERE id = ?""", String.class, sesionB.getId());
        assertThat(fila).isEqualTo("PROGRAMADA|true");
        assertThat(horarioA.getId()).isNotEqualTo(horarioB.getId());
    }

    @Test
    void adminAbreSesionDeCualquierDocente() {
        autenticar("admin.svc", Rol.ADMIN);
        Horario horario = guardarHorario("doc.sesion.svc.b");
        SesionClase sesion = sesionClaseService.obtenerOCrearSesion(horario.getId(), LocalDate.of(2026, 8, 19));

        var respuesta = sesionClaseService.abrirSesion(sesion.getId());

        assertThat(respuesta.estado()).isEqualTo(SesionClaseEstado.ABIERTA);
        assertThat(respuesta.horaApertura()).isNotNull();
    }

    @Test
    void cerradaNoSeAbreYPermaneceCerrada() {
        autenticar("admin.svc", Rol.ADMIN);
        Horario horario = guardarHorario("doc.sesion.svc.a");
        SesionClase sesion = sesionClaseService.obtenerOCrearSesion(horario.getId(), LocalDate.of(2026, 8, 19));
        jdbcTemplate.update("UPDATE sesiones_clase SET estado = 'CERRADA' WHERE id = ?", sesion.getId());

        assertThatThrownBy(() -> sesionClaseService.abrirSesion(sesion.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CERRADA");

        String estado = jdbcTemplate.queryForObject(
                "SELECT estado FROM sesiones_clase WHERE id = ?", String.class, sesion.getId());
        assertThat(estado).isEqualTo("CERRADA");
    }

    @Test
    void canceladaNoSeAbreYPermaneceCancelada() {
        autenticar("admin.svc", Rol.ADMIN);
        Horario horario = guardarHorario("doc.sesion.svc.a");
        SesionClase sesion = sesionClaseService.obtenerOCrearSesion(horario.getId(), LocalDate.of(2026, 8, 19));
        jdbcTemplate.update("UPDATE sesiones_clase SET estado = 'CANCELADA' WHERE id = ?", sesion.getId());

        assertThatThrownBy(() -> sesionClaseService.abrirSesion(sesion.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CANCELADA");

        String estado = jdbcTemplate.queryForObject(
                "SELECT estado FROM sesiones_clase WHERE id = ?", String.class, sesion.getId());
        assertThat(estado).isEqualTo("CANCELADA");
    }

    @Test
    void reaperturaDeSesionAbiertaConservaHoraAperturaOriginal() {
        autenticar("admin.svc", Rol.ADMIN);
        Horario horario = guardarHorario("doc.sesion.svc.a");
        SesionClase sesion = sesionClaseService.obtenerOCrearSesion(horario.getId(), LocalDate.of(2026, 8, 19));

        OffsetDateTime primera = sesionClaseService.abrirSesion(sesion.getId()).horaApertura();
        var segunda = sesionClaseService.abrirSesion(sesion.getId());

        assertThat(segunda.estado()).isEqualTo(SesionClaseEstado.ABIERTA);
        assertThat(segunda.horaApertura().toInstant())
                .isCloseTo(primera.toInstant(), within(1, ChronoUnit.MILLIS));
        String enBd = jdbcTemplate.queryForObject("""
                SELECT to_char(hora_apertura AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"')
                FROM sesiones_clase WHERE id = ?""", String.class, sesion.getId());
        assertThat(OffsetDateTime.parse(enBd).toInstant())
                .isCloseTo(primera.toInstant(), within(1, ChronoUnit.MILLIS));
    }

    @Test
    void idInexistenteNoCreaNiAbreSesion() {
        autenticar("admin.svc", Rol.ADMIN);
        assertThatThrownBy(() -> sesionClaseService.abrirSesion(999999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sesión no encontrada");
        assertThat(sesionClaseRepository.count()).isZero();
    }

    // ===================== Prompt 6.4 — listados (PostgreSQL real) =====================

    @Test
    void docenteListaSoloSusSesionesEnBd() {
        Horario horarioA = guardarHorario("doc.sesion.svc.a");
        Horario horarioB = guardarHorario("doc.sesion.svc.b");
        long sesionA = sesionClaseService.obtenerOCrearSesion(horarioA.getId(), LocalDate.of(2026, 8, 19)).getId();
        long sesionB = sesionClaseService.obtenerOCrearSesion(horarioB.getId(), LocalDate.of(2026, 8, 19)).getId();
        assertThat(sesionA).isNotEqualTo(sesionB);
        autenticar("doc.sesion.svc.a", Rol.DOCENTE);

        var resultado = sesionClaseService.listarSesiones(null, null, null, null, null);

        assertThat(resultado).extracting(SesionClaseResponse::id).containsExactly(sesionA);
    }

    @Test
    void docenteIgnoraFiltroDocenteAjenoEnBd() {
        Horario horarioA = guardarHorario("doc.sesion.svc.a");
        Horario horarioB = guardarHorario("doc.sesion.svc.b");
        long sesionA = sesionClaseService.obtenerOCrearSesion(horarioA.getId(), LocalDate.of(2026, 8, 19)).getId();
        sesionClaseService.obtenerOCrearSesion(horarioB.getId(), LocalDate.of(2026, 8, 19));
        autenticar("doc.sesion.svc.a", Rol.DOCENTE);

        var resultado = sesionClaseService.listarSesiones(null, 999999L, null, null, null);

        assertThat(resultado).extracting(SesionClaseResponse::id).containsExactly(sesionA);
    }

    @Test
    void docenteListaActivasSoloDeSusHorariosEnBd() {
        Horario horarioA = guardarHorario("doc.sesion.svc.a");
        Horario horarioB = guardarHorario("doc.sesion.svc.b");
        long sesionA = sesionClaseService.obtenerOCrearSesion(horarioA.getId(), LocalDate.of(2026, 8, 19)).getId();
        long sesionB = sesionClaseService.obtenerOCrearSesion(horarioB.getId(), LocalDate.of(2026, 8, 19)).getId();
        autenticar("admin.svc", Rol.ADMIN);
        sesionClaseService.abrirSesion(sesionA);
        sesionClaseService.abrirSesion(sesionB);
        autenticar("doc.sesion.svc.a", Rol.DOCENTE);

        var resultado = sesionClaseService.listarSesionesActivas();

        assertThat(resultado).extracting(SesionClaseResponse::id).containsExactly(sesionA);
    }

    @Test
    void adminListaConFiltroDocenteEnBd() {
        Horario horarioA = guardarHorario("doc.sesion.svc.a");
        Horario horarioB = guardarHorario("doc.sesion.svc.b");
        sesionClaseService.obtenerOCrearSesion(horarioA.getId(), LocalDate.of(2026, 8, 19));
        long sesionB = sesionClaseService.obtenerOCrearSesion(horarioB.getId(), LocalDate.of(2026, 8, 19)).getId();
        autenticar("admin.svc", Rol.ADMIN);

        var resultado = sesionClaseService.listarSesiones(null, horarioB.getDocente().getId(),
                null, null, null);

        assertThat(resultado).extracting(SesionClaseResponse::id).containsExactly(sesionB);
    }

    @Test
    void adminListaConFiltroEstadoEnBd() {
        autenticar("admin.svc", Rol.ADMIN);
        Horario horarioA = guardarHorario("doc.sesion.svc.a");
        Horario horarioB = guardarHorario("doc.sesion.svc.b");
        long abierta = sesionClaseService.obtenerOCrearSesion(horarioA.getId(), LocalDate.of(2026, 8, 19)).getId();
        sesionClaseService.obtenerOCrearSesion(horarioB.getId(), LocalDate.of(2026, 8, 19));
        sesionClaseService.abrirSesion(abierta);

        var resultado = sesionClaseService.listarSesiones(null, null, null, null,
                SesionClaseEstado.ABIERTA);

        assertThat(resultado).extracting(SesionClaseResponse::id).containsExactly(abierta);
    }

    @Test
    void adminBuscaSesionPorIdCualquieraEnBdConResumenes() {
        autenticar("admin.svc", Rol.ADMIN);
        Horario horario = guardarHorario("doc.sesion.svc.a");
        long sesionId = sesionClaseService.obtenerOCrearSesion(horario.getId(), LocalDate.of(2026, 8, 19)).getId();

        var respuesta = sesionClaseService.buscarSesionPorId(sesionId);

        assertThat(respuesta.id()).isEqualTo(sesionId);
        assertThat(respuesta.horarioId()).isEqualTo(horario.getId());
        assertThat(respuesta.estado()).isEqualTo(SesionClaseEstado.PROGRAMADA);
        assertThat(respuesta.curso().nombre()).isEqualTo("Matemática SVC");
        assertThat(respuesta.seccion().nombre()).isEqualTo("A");
        assertThat(respuesta.docente().id()).isEqualTo(horario.getDocente().getId());
    }

    @Test
    void docenteBuscaSuSesionPorIdEnBd() {
        Horario horarioA = guardarHorario("doc.sesion.svc.a");
        long sesionId = sesionClaseService.obtenerOCrearSesion(horarioA.getId(), LocalDate.of(2026, 8, 19)).getId();
        autenticar("doc.sesion.svc.a", Rol.DOCENTE);

        var respuesta = sesionClaseService.buscarSesionPorId(sesionId);

        assertThat(respuesta.id()).isEqualTo(sesionId);
        assertThat(respuesta.horarioId()).isEqualTo(horarioA.getId());
    }

    @Test
    void docenteBuscaSesionAjenaLanzaForbiddenEnBd() {
        Horario horarioA = guardarHorario("doc.sesion.svc.a");
        Horario horarioB = guardarHorario("doc.sesion.svc.b");
        long sesionB = sesionClaseService.obtenerOCrearSesion(horarioB.getId(), LocalDate.of(2026, 8, 19)).getId();
        assertThat(horarioA.getId()).isNotEqualTo(horarioB.getId());
        autenticar("doc.sesion.svc.a", Rol.DOCENTE);

        assertThatThrownBy(() -> sesionClaseService.buscarSesionPorId(sesionB))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("otro docente");
    }
}