package com.aulaia.repository;

import com.aulaia.entity.Asistencia;
import com.aulaia.entity.Curso;
import com.aulaia.entity.Docente;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.Grado;
import com.aulaia.entity.Horario;
import com.aulaia.entity.MetodoRegistro;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.SesionClase;
import com.aulaia.entity.SesionClaseEstado;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración de {@link AsistenciaRepository} contra PostgreSQL real
 * (perfil {@code itest}, 07-PLAN Prompt 7.1 §27–§42).
 *
 * <p>Deshabilitadas por defecto — no impactan {@code mvn test} normal.
 * Ejecución explícita (PowerShell):
 * <pre>
 * $env:DB_HOST="localhost"; $env:DB_PORT="5433"; $env:DB_NAME="aulaia_db"
 * $env:DB_USERNAME="aulaia_user"; $env:DB_PASSWORD="TU_PASSWORD_LOCAL"
 * .\mvnw.cmd test "-Dtest=AsistenciaRepositoryTest" "-Daulaia.itest=true"
 * </pre>
 *
 * <p>Cada test es {@code @Transactional}: revierte todos sus cambios al finalizar.
 * No asume BD vacía. Usa fixtures únicos. Sin {@code TRUNCATE CASCADE} ni
 * {@code deleteAll} global.
 *
 * <p>Cubre: FK sesión, FK estudiante, UNIQUE sesion+estudiante, casos UNIQUE
 * válidos, CHECK estado, CHECK metodo, DEFAULT fecha_hora, defaults timestamps,
 * observacion null, roundtrip de enums.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("itest")
@EnabledIfSystemProperty(named = "aulaia.itest", matches = "true")
class AsistenciaRepositoryTest {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.AuditoriaRepository auditoriaRepository;

    @Autowired
    private SesionClaseRepository sesionClaseRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private SeccionRepository seccionRepository;

    @Autowired
    private GradoRepository gradoRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    @PersistenceContext
    private EntityManager em;

    // ─── Contador para unicidad de fixtures ──────────────────────────────
    private static int contador;

    // ─── Helpers de fixture ───────────────────────────────────────────────

    private Grado guardarGrado() {
        Grado g = new Grado();
        g.setNombre("6.º Primaria Test " + (++contador));
        return gradoRepository.saveAndFlush(g);
    }

    private Seccion guardarSeccion() {
        Seccion s = new Seccion();
        s.setGrado(guardarGrado());
        s.setNombre("A" + contador);
        s.setPeriodoAcademico("2026-" + contador);
        return seccionRepository.saveAndFlush(s);
    }

    private Curso guardarCurso() {
        Curso c = new Curso();
        c.setNombre("Curso Asist Test " + (++contador));
        return cursoRepository.saveAndFlush(c);
    }

    private Usuario guardarUsuario() {
        Usuario u = new Usuario();
        u.setUsername("doc.asist.test." + (++contador));
        u.setPasswordHash("$2a$10$hashFicticioAsistTest1234567890000");
        u.setRol(Rol.DOCENTE);
        u.setActivo(true);
        return usuarioRepository.saveAndFlush(u);
    }

    private Docente guardarDocente() {
        Docente d = new Docente();
        d.setUsuario(guardarUsuario());
        d.setNombres("Docente Asist");
        d.setApellidos("Prueba " + contador);
        return docenteRepository.saveAndFlush(d);
    }

    private Horario guardarHorario(Curso curso, Seccion seccion, Docente docente) {
        Horario h = new Horario();
        h.setCurso(curso);
        h.setSeccion(seccion);
        h.setDocente(docente);
        h.setDiaSemana((short) 2);
        h.setHoraInicio(LocalTime.of(9, 0));
        h.setHoraFin(LocalTime.of(10, 30));
        return horarioRepository.saveAndFlush(h);
    }

    private SesionClase guardarSesion(Horario horario) {
        SesionClase s = new SesionClase();
        s.setHorario(horario);
        s.setFecha(LocalDate.of(2026, 8, 19).plusDays(contador));
        s.setEstado(SesionClaseEstado.ABIERTA);
        return sesionClaseRepository.saveAndFlush(s);
    }

    private Estudiante guardarEstudiante(Seccion seccion) {
        Estudiante e = new Estudiante();
        e.setCodigo("EST-ASIST-" + (++contador));
        e.setQrToken("AULAIA:STUDENT:ASIST" + contador);
        e.setNombres("Estudiante Asist");
        e.setApellidos("Prueba " + contador);
        e.setSeccion(seccion);
        return estudianteRepository.saveAndFlush(e);
    }

    /** Crea un grafo completo: horario, sesión y estudiante de la misma sección. */
    private Fixture crearFixture() {
        Seccion seccion = guardarSeccion();
        Horario horario = guardarHorario(guardarCurso(), seccion, guardarDocente());
        SesionClase sesion = guardarSesion(horario);
        Estudiante estudiante = guardarEstudiante(seccion);
        return new Fixture(sesion, estudiante);
    }

    private Asistencia asistenciaValida(SesionClase sesion, Estudiante estudiante) {
        Asistencia a = new Asistencia();
        a.setSesionClase(sesion);
        a.setEstudiante(estudiante);
        a.setFechaHora(OffsetDateTime.now());
        a.setEstado(EstadoAsistencia.PRESENTE);
        a.setMetodo(MetodoRegistro.QR);
        return a;
    }

    record Fixture(SesionClase sesion, Estudiante estudiante) {}

    // ─── Test §28: Creación válida ─────────────────────────────────────────

    @Test
    void guardarAsistenciaValidaPersiste() {
        Fixture f = crearFixture();
        Asistencia guardada = asistenciaRepository.saveAndFlush(
                asistenciaValida(f.sesion(), f.estudiante()));

        assertThat(guardada.getId()).isNotNull().isPositive();
        assertThat(guardada.getSesionClase().getId()).isEqualTo(f.sesion().getId());
        assertThat(guardada.getEstudiante().getId()).isEqualTo(f.estudiante().getId());
        assertThat(guardada.getEstado()).isEqualTo(EstadoAsistencia.PRESENTE);
        assertThat(guardada.getMetodo()).isEqualTo(MetodoRegistro.QR);
        assertThat(guardada.getFechaHora()).isNotNull();
        assertThat(guardada.getCreatedAt()).isNotNull();
        assertThat(guardada.getUpdatedAt()).isNotNull();
    }

    // ─── Test §29: FK sesión ──────────────────────────────────────────────

    @Test
    void sesionInexistenteEsRechazadaPorFK() {
        Fixture f = crearFixture();

        SesionClase fantasma = new SesionClase();
        fantasma.setId(999_999_991L);

        Asistencia a = new Asistencia();
        a.setSesionClase(fantasma);
        a.setEstudiante(f.estudiante());
        a.setFechaHora(OffsetDateTime.now());
        a.setEstado(EstadoAsistencia.PRESENTE);
        a.setMetodo(MetodoRegistro.QR);

        assertThatThrownBy(() -> asistenciaRepository.saveAndFlush(a))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ─── Test §30: FK estudiante ──────────────────────────────────────────

    @Test
    void estudianteInexistenteEsRechazadoPorFK() {
        Fixture f = crearFixture();

        Estudiante fantasma = new Estudiante();
        fantasma.setId(999_999_992L);

        Asistencia a = new Asistencia();
        a.setSesionClase(f.sesion());
        a.setEstudiante(fantasma);
        a.setFechaHora(OffsetDateTime.now());
        a.setEstado(EstadoAsistencia.PRESENTE);
        a.setMetodo(MetodoRegistro.QR);

        assertThatThrownBy(() -> asistenciaRepository.saveAndFlush(a))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ─── Test §31: UNIQUE duplicado ────────────────────────────────────────

    @Test
    void mismoEstudianteMismaSesionEsRechazadoPorUnique() {
        Fixture f = crearFixture();

        // Primera asistencia OK
        asistenciaRepository.saveAndFlush(asistenciaValida(f.sesion(), f.estudiante()));
        em.flush();

        // Segunda asistencia: mismo estudiante + misma sesión → UNIQUE violation
        Asistencia duplicada = asistenciaValida(f.sesion(), f.estudiante());
        assertThatThrownBy(() -> asistenciaRepository.saveAndFlush(duplicada))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ─── Test §32: Casos UNIQUE válidos ───────────────────────────────────

    @Test
    void mismaSesionEstudianteDiferentePermitido() {
        Fixture f = crearFixture();
        Estudiante e2 = guardarEstudiante(f.sesion().getHorario().getSeccion());

        asistenciaRepository.saveAndFlush(asistenciaValida(f.sesion(), f.estudiante()));
        Asistencia a2 = asistenciaValida(f.sesion(), e2);
        Asistencia guardada2 = asistenciaRepository.saveAndFlush(a2);

        assertThat(guardada2.getId()).isNotNull();
    }

    @Test
    void mismoEstudianteSesionDiferentePermitido() {
        Fixture f = crearFixture();

        // Segunda sesión para el mismo horario (fecha diferente)
        SesionClase sesion2 = new SesionClase();
        sesion2.setHorario(f.sesion().getHorario());
        sesion2.setFecha(LocalDate.of(2030, 1, 1).plusDays(++contador));
        sesion2.setEstado(SesionClaseEstado.ABIERTA);
        sesion2 = sesionClaseRepository.saveAndFlush(sesion2);

        asistenciaRepository.saveAndFlush(asistenciaValida(f.sesion(), f.estudiante()));
        Asistencia a2 = asistenciaValida(sesion2, f.estudiante());
        Asistencia guardada2 = asistenciaRepository.saveAndFlush(a2);

        assertThat(guardada2.getId()).isNotNull();
    }

    // ─── Test §33: CHECK estado inválido ──────────────────────────────────

    @Test
    void estadoInvalidoEsRechazadoPorCheck() {
        Fixture f = crearFixture();
        em.flush(); // asegura que los fixtures están en BD

        assertThatThrownBy(() -> {
            em.createNativeQuery(
                    "INSERT INTO asistencias " +
                    "(sesion_clase_id, estudiante_id, fecha_hora, estado, metodo) " +
                    "VALUES (:sid, :eid, CURRENT_TIMESTAMP, 'OTRO', 'QR')")
                    .setParameter("sid", f.sesion().getId())
                    .setParameter("eid", f.estudiante().getId())
                    .executeUpdate();
            em.flush();
        }).isInstanceOf(Exception.class); // ck_asistencia_estado
    }

    // ─── Test §34: CHECK metodo inválido ──────────────────────────────────

    @Test
    void metodoInvalidoEsRechazadoPorCheck() {
        Fixture f = crearFixture();
        em.flush();

        assertThatThrownBy(() -> {
            em.createNativeQuery(
                    "INSERT INTO asistencias " +
                    "(sesion_clase_id, estudiante_id, fecha_hora, estado, metodo) " +
                    "VALUES (:sid, :eid, CURRENT_TIMESTAMP, 'PRESENTE', 'OTRO')")
                    .setParameter("sid", f.sesion().getId())
                    .setParameter("eid", f.estudiante().getId())
                    .executeUpdate();
            em.flush();
        }).isInstanceOf(Exception.class); // ck_asistencia_metodo
    }

    // ─── Test §35: DEFAULT fecha_hora ─────────────────────────────────────

    @Test
    void defaultFechaHoraAsignadaPorBD() {
        Fixture f = crearFixture();
        em.flush();

        // INSERT nativo sin fecha_hora → BD aplica DEFAULT CURRENT_TIMESTAMP
        em.createNativeQuery(
                "INSERT INTO asistencias " +
                "(sesion_clase_id, estudiante_id, estado, metodo) " +
                "VALUES (:sid, :eid, 'PRESENTE', 'QR')")
                .setParameter("sid", f.sesion().getId())
                .setParameter("eid", f.estudiante().getId())
                .executeUpdate();
        em.flush();

        Object fechaHora = em.createNativeQuery(
                "SELECT fecha_hora FROM asistencias " +
                "WHERE sesion_clase_id = :sid AND estudiante_id = :eid")
                .setParameter("sid", f.sesion().getId())
                .setParameter("eid", f.estudiante().getId())
                .getSingleResult();

        assertThat(fechaHora).isNotNull();
    }

    // ─── Test §36: DEFAULT created_at / updated_at ────────────────────────

    @Test
    void defaultsTimestampsNoNulos() {
        Fixture f = crearFixture();
        Asistencia guardada = asistenciaRepository.saveAndFlush(
                asistenciaValida(f.sesion(), f.estudiante()));

        assertThat(guardada.getCreatedAt()).isNotNull();
        assertThat(guardada.getUpdatedAt()).isNotNull();
    }

    // ─── Test §37: observacion null ──────────────────────────────────────

    @Test
    void observacionNullEsValida() {
        Fixture f = crearFixture();
        Asistencia a = asistenciaValida(f.sesion(), f.estudiante());
        a.setObservacion(null);
        Asistencia guardada = asistenciaRepository.saveAndFlush(a);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getObservacion()).isNull();
    }

    // ─── Test §38: observacion longitud 500 ──────────────────────────────

    @Test
    void observacion500CaracteresPermitida() {
        Fixture f = crearFixture();
        String obs500 = "O".repeat(500);
        Asistencia a = asistenciaValida(f.sesion(), f.estudiante());
        a.setObservacion(obs500);
        Asistencia guardada = asistenciaRepository.saveAndFlush(a);

        assertThat(guardada.getObservacion()).hasSize(500);
    }

    // ─── Test §39: Roundtrip de enums ─────────────────────────────────────

    @Test
    void roundtripEstadoPresenteYMetodoQR() {
        Fixture f = crearFixture();
        Asistencia a = asistenciaValida(f.sesion(), f.estudiante());
        a.setEstado(EstadoAsistencia.PRESENTE);
        a.setMetodo(MetodoRegistro.QR);
        Asistencia guardada = asistenciaRepository.saveAndFlush(a);
        em.clear(); // detach para forzar recarga desde BD

        Asistencia recargada = asistenciaRepository.findById(guardada.getId()).orElseThrow();
        assertThat(recargada.getEstado()).isEqualTo(EstadoAsistencia.PRESENTE);
        assertThat(recargada.getMetodo()).isEqualTo(MetodoRegistro.QR);
    }
}
