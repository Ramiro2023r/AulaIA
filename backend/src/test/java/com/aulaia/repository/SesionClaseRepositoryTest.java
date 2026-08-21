package com.aulaia.repository;

import com.aulaia.entity.Curso;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Grado;
import com.aulaia.entity.Horario;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.SesionClase;
import com.aulaia.entity.SesionClaseEstado;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración de {@link SesionClaseRepository} contra
 * PostgreSQL real (perfil {@code itest}), siguiendo el patrón del
 * proyecto.
 *
 * <p>Deshabilitadas por defecto. Ejecución explícita (PowerShell):
 * <pre>
 * $env:DB_HOST="localhost"; $env:DB_PORT="5433"; $env:DB_NAME="aulaia_db"
 * $env:DB_USERNAME="aulaia_user"; $env:DB_PASSWORD="TU_PASSWORD_LOCAL"
 * .\mvnw.cmd test "-Dtest=SesionClaseRepositoryTest" "-Daulaia.itest=true"
 * </pre>
 *
 * <p>Transaccionales: cada test revierte sus cambios. Verifican las
 * restricciones físicas de {@code sesiones_clase} (04-BD §7.1): FK a
 * horarios, UNIQUE (horario_id, fecha), default de estado PROGRAMADA,
 * nullability real de hora_apertura/hora_cierre y timestamps. Datos
 * ficticios, nunca reales.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("itest")
@EnabledIfSystemProperty(named = "aulaia.itest", matches = "true")
class SesionClaseRepositoryTest {

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

    @PersistenceContext
    private EntityManager entityManager;

    private static int contadorUsuarios;

    private Curso guardarCurso() {
        return guardarCurso("Matemática");
    }

    private Curso guardarCurso(String nombre) {
        Curso curso = new Curso();
        curso.setNombre(nombre);
        return cursoRepository.saveAndFlush(curso);
    }

    private Seccion guardarSeccion() {
        return guardarSeccion("A");
    }

    private Seccion guardarSeccion(String nombreSeccion) {
        Grado grado = new Grado();
        grado.setNombre("6.º Primaria");
        grado = gradoRepository.saveAndFlush(grado);
        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setNombre(nombreSeccion);
        seccion.setPeriodoAcademico("2026");
        return seccionRepository.saveAndFlush(seccion);
    }

    private Docente guardarDocente() {
        return guardarDocente("filtro");
    }

    private Docente guardarDocente(String sufijo) {
        Usuario usuario = new Usuario();
        usuario.setUsername("doc.sesion.test." + sufijo + (++contadorUsuarios));
        usuario.setPasswordHash("$2a$10$hashFicticioParaPruebasSoloLocal1234567890");
        usuario.setRol(Rol.DOCENTE);
        usuario.setActivo(true);
        usuario = usuarioRepository.saveAndFlush(usuario);
        Docente docente = new Docente();
        docente.setUsuario(usuario);
        docente.setNombres("Docente");
        docente.setApellidos("De Prueba");
        return docenteRepository.saveAndFlush(docente);
    }

    private Horario guardarHorario() {
        return guardarHorario(guardarCurso(), guardarSeccion(), guardarDocente());
    }

    private Horario guardarHorario(Curso curso, Seccion seccion, Docente docente) {
        Horario horario = new Horario();
        horario.setCurso(curso);
        horario.setSeccion(seccion);
        horario.setDocente(docente);
        horario.setDiaSemana((short) 1);
        horario.setHoraInicio(LocalTime.of(9, 0));
        horario.setHoraFin(LocalTime.of(10, 30));
        return horarioRepository.saveAndFlush(horario);
    }

    private SesionClase sesion(Horario horario, LocalDate fecha) {
        SesionClase sesion = new SesionClase();
        sesion.setHorario(horario);
        sesion.setFecha(fecha);
        sesion.setEstado(SesionClaseEstado.PROGRAMADA);
        return sesion;
    }

    @Test
    void guardarSesionValida() {
        SesionClase guardada = sesionClaseRepository.saveAndFlush(
                sesion(guardarHorario(), LocalDate.of(2026, 8, 19)));

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getFecha()).isEqualTo(LocalDate.of(2026, 8, 19));
        assertThat(guardada.getEstado()).isEqualTo(SesionClaseEstado.PROGRAMADA);
        assertThat(guardada.getCreatedAt()).isNotNull();
        assertThat(guardada.getUpdatedAt()).isNotNull();
    }

    @Test
    void buscarSesionPorId() {
        SesionClase guardada = sesionClaseRepository.saveAndFlush(
                sesion(guardarHorario(), LocalDate.of(2026, 8, 20)));

        Optional<SesionClase> encontrada = sesionClaseRepository.findById(guardada.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getFecha()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void relacionHorarioValida() {
        Horario horario = guardarHorario();
        SesionClase guardada = sesionClaseRepository.saveAndFlush(
                sesion(horario, LocalDate.of(2026, 8, 21)));

        assertThat(guardada.getHorario().getId()).isEqualTo(horario.getId());
        assertThat(guardada.getHorario().getDiaSemana()).isEqualTo((short) 1);
    }

    @Test
    void horarioInexistenteEsRechazado() {
        Horario horarioFantasma = new Horario();
        horarioFantasma.setId(999999L);

        assertThatThrownBy(() -> sesionClaseRepository.saveAndFlush(
                sesion(horarioFantasma, LocalDate.of(2026, 8, 19))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void mismoHorarioMismaFechaEsRechazado() {
        Horario horario = guardarHorario();
        LocalDate fecha = LocalDate.of(2026, 8, 19);
        sesionClaseRepository.saveAndFlush(sesion(horario, fecha));

        assertThatThrownBy(() -> sesionClaseRepository.saveAndFlush(sesion(horario, fecha)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void mismoHorarioOtraFechaPermitido() {
        Horario horario = guardarHorario();
        sesionClaseRepository.saveAndFlush(sesion(horario, LocalDate.of(2026, 8, 19)));

        SesionClase segunda = sesionClaseRepository.saveAndFlush(
                sesion(horario, LocalDate.of(2026, 8, 20)));

        assertThat(segunda.getId()).isNotNull();
        assertThat(segunda.getFecha()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void otroHorarioMismaFechaPermitido() {
        Horario horarioA = guardarHorario();
        Horario horarioB = guardarHorario();
        LocalDate fecha = LocalDate.of(2026, 8, 19);
        sesionClaseRepository.saveAndFlush(sesion(horarioA, fecha));

        SesionClase segunda = sesionClaseRepository.saveAndFlush(sesion(horarioB, fecha));

        assertThat(segunda.getId()).isNotNull();
        assertThat(segunda.getHorario().getId()).isEqualTo(horarioB.getId());
    }

    @Test
    void estadosValidosPersisten() {
        Horario horario = guardarHorario();
        int idx = 0;
        for (SesionClaseEstado estado : SesionClaseEstado.values()) {
            SesionClase sesion = sesion(horario, LocalDate.of(2026, 8, 1).plusDays(idx++));
            sesion.setEstado(estado);
            SesionClase guardada = sesionClaseRepository.saveAndFlush(sesion);
            assertThat(guardada.getEstado()).isEqualTo(estado);
        }
    }

    @Test
    void defaultDeEstadoEsProgramada() {
        Horario horario = guardarHorario();

        entityManager.createNativeQuery("""
                        INSERT INTO sesiones_clase (horario_id, fecha)
                        VALUES (:horario, '2026-08-19')""")
                .setParameter("horario", horario.getId())
                .executeUpdate();

        Object estado = entityManager.createNativeQuery("""
                        SELECT estado FROM sesiones_clase WHERE horario_id = :horario AND fecha = '2026-08-19'""")
                .setParameter("horario", horario.getId())
                .getSingleResult();

        assertThat((String) estado).isEqualTo("PROGRAMADA");
    }

    @Test
    void timestampsSeCrean() {
        SesionClase guardada = sesionClaseRepository.saveAndFlush(
                sesion(guardarHorario(), LocalDate.of(2026, 8, 22)));

        assertThat(guardada.getCreatedAt()).isNotNull();
        assertThat(guardada.getUpdatedAt()).isNotNull();
    }

    @Test
    void nullabilityRealDeHorasAperturaYCierre() {
        SesionClase guardada = sesionClaseRepository.saveAndFlush(
                sesion(guardarHorario(), LocalDate.of(2026, 8, 23)));
        assertThat(guardada.getHoraApertura()).isNull();
        assertThat(guardada.getHoraCierre()).isNull();

        guardada.setHoraApertura(OffsetDateTime.now());
        guardada.setHoraCierre(OffsetDateTime.now().plusHours(2));
        SesionClase actualizada = sesionClaseRepository.saveAndFlush(guardada);

        assertThat(actualizada.getHoraApertura()).isNotNull();
        assertThat(actualizada.getHoraCierre()).isNotNull();
    }

    @Test
    void findByHorarioIdAndFecha() {
        Horario horario = guardarHorario();
        LocalDate fecha = LocalDate.of(2026, 8, 24);
        sesionClaseRepository.saveAndFlush(sesion(horario, fecha));

        Optional<SesionClase> encontrada = sesionClaseRepository.findByHorarioIdAndFecha(horario.getId(), fecha);
        assertThat(encontrada).isPresent();
        assertThat(sesionClaseRepository.existsByHorarioIdAndFecha(horario.getId(), fecha)).isTrue();
        assertThat(sesionClaseRepository.existsByHorarioIdAndFecha(horario.getId(), fecha.plusDays(1))).isFalse();
    }

    // ===================== Prompt 6.4 — buscarConFiltros =====================

    @Test
    void buscarConFiltrosSinFiltrosDevuelveTodoEnOrdenEstable() {
        Horario horarioA = guardarHorario();
        Horario horarioB = guardarHorario();
        LocalDate fecha = LocalDate.of(2026, 8, 19);
        sesionClaseRepository.saveAndFlush(sesion(horarioA, fecha));
        sesionClaseRepository.saveAndFlush(sesion(horarioB, fecha.plusDays(1)));

        var resultado = sesionClaseRepository.buscarConFiltros(null, null, null, null, null);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getId()).isLessThan(resultado.get(1).getId());
    }

    @Test
    void buscarConFiltrosPorFecha() {
        Horario horarioA = guardarHorario();
        Horario horarioB = guardarHorario();
        LocalDate del19 = LocalDate.of(2026, 8, 19);
        LocalDate del20 = LocalDate.of(2026, 8, 20);
        sesionClaseRepository.saveAndFlush(sesion(horarioA, del19));
        sesionClaseRepository.saveAndFlush(sesion(horarioB, del20));

        var resultado = sesionClaseRepository.buscarConFiltros(del19, null, null, null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getFecha()).isEqualTo(del19);
    }

    @Test
    void buscarConFiltrosPorDocente() {
        Curso curso = guardarCurso();
        Seccion seccion = guardarSeccion();
        Docente docenteA = guardarDocente("a");
        Docente docenteB = guardarDocente("b");
        Horario horarioA = guardarHorario(curso, seccion, docenteA);
        Horario horarioB = guardarHorario(curso, seccion, docenteB);
        LocalDate fecha = LocalDate.of(2026, 8, 19);
        sesionClaseRepository.saveAndFlush(sesion(horarioA, fecha));
        sesionClaseRepository.saveAndFlush(sesion(horarioB, fecha));

        var resultado = sesionClaseRepository.buscarConFiltros(null, docenteA.getId(), null, null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getHorario().getDocente().getId()).isEqualTo(docenteA.getId());
    }

    @Test
    void buscarConFiltrosPorSeccion() {
        Curso curso = guardarCurso();
        Seccion seccionA = guardarSeccion("A");
        Seccion seccionB = guardarSeccion("B");
        Docente docente = guardarDocente();
        Horario horarioA = guardarHorario(curso, seccionA, docente);
        Horario horarioB = guardarHorario(curso, seccionB, docente);
        LocalDate fecha = LocalDate.of(2026, 8, 19);
        sesionClaseRepository.saveAndFlush(sesion(horarioA, fecha));
        sesionClaseRepository.saveAndFlush(sesion(horarioB, fecha));

        var resultado = sesionClaseRepository.buscarConFiltros(null, null, seccionA.getId(), null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getHorario().getSeccion().getId()).isEqualTo(seccionA.getId());
    }

    @Test
    void buscarConFiltrosPorCurso() {
        Curso cursoA = guardarCurso("Matemática");
        Curso cursoB = guardarCurso("Comunicación");
        Seccion seccion = guardarSeccion();
        Docente docente = guardarDocente();
        Horario horarioA = guardarHorario(cursoA, seccion, docente);
        Horario horarioB = guardarHorario(cursoB, seccion, docente);
        LocalDate fecha = LocalDate.of(2026, 8, 19);
        sesionClaseRepository.saveAndFlush(sesion(horarioA, fecha));
        sesionClaseRepository.saveAndFlush(sesion(horarioB, fecha));

        var resultado = sesionClaseRepository.buscarConFiltros(null, null, null, cursoB.getId(), null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getHorario().getCurso().getId()).isEqualTo(cursoB.getId());
    }

    @Test
    void buscarConFiltrosPorEstado() {
        Horario horarioA = guardarHorario();
        Horario horarioB = guardarHorario();
        LocalDate fecha = LocalDate.of(2026, 8, 19);
        SesionClase abierta = sesionClaseRepository.saveAndFlush(sesion(horarioA, fecha));
        abierta.setEstado(SesionClaseEstado.ABIERTA);
        sesionClaseRepository.saveAndFlush(abierta);
        sesionClaseRepository.saveAndFlush(sesion(horarioB, fecha));

        var resultado = sesionClaseRepository.buscarConFiltros(null, null, null, null,
                SesionClaseEstado.ABIERTA);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(SesionClaseEstado.ABIERTA);
    }

    @Test
    void buscarConFiltrosCombinaFiltrosEnAnd() {
        Curso cursoA = guardarCurso("Matemática");
        Curso cursoB = guardarCurso("Comunicación");
        Seccion seccion = guardarSeccion();
        Docente docente = guardarDocente();
        Horario horarioA = guardarHorario(cursoA, seccion, docente);
        Horario horarioB = guardarHorario(cursoB, seccion, docente);
        LocalDate fecha = LocalDate.of(2026, 8, 19);
        sesionClaseRepository.saveAndFlush(sesion(horarioA, fecha));
        SesionClase otraFecha = sesionClaseRepository.saveAndFlush(sesion(horarioA, fecha.plusDays(1)));
        otraFecha.setEstado(SesionClaseEstado.ABIERTA);
        sesionClaseRepository.saveAndFlush(otraFecha);
        sesionClaseRepository.saveAndFlush(sesion(horarioB, fecha));

        var resultado = sesionClaseRepository.buscarConFiltros(fecha, docente.getId(), null,
                cursoA.getId(), SesionClaseEstado.PROGRAMADA);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getHorario().getCurso().getId()).isEqualTo(cursoA.getId());
        assertThat(resultado.get(0).getFecha()).isEqualTo(fecha);
        assertThat(resultado.get(0).getEstado()).isEqualTo(SesionClaseEstado.PROGRAMADA);
    }

    @Test
    void buscarConFiltrosSinCoincidenciasDevuelveVacio() {
        Horario horario = guardarHorario();
        sesionClaseRepository.saveAndFlush(sesion(horario, LocalDate.of(2026, 8, 19)));

        var resultado = sesionClaseRepository.buscarConFiltros(LocalDate.of(2030, 1, 1),
                null, null, null, SesionClaseEstado.CERRADA);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscarConFiltrosActivasSoloDevuelveAbiertas() {
        Horario horarioA = guardarHorario();
        Horario horarioB = guardarHorario();
        LocalDate fecha = LocalDate.of(2026, 8, 19);
        SesionClase abierta = sesionClaseRepository.saveAndFlush(sesion(horarioA, fecha));
        abierta.setEstado(SesionClaseEstado.ABIERTA);
        sesionClaseRepository.saveAndFlush(abierta);
        SesionClase programada = sesionClaseRepository.saveAndFlush(sesion(horarioB, fecha));
        programada.setEstado(SesionClaseEstado.PROGRAMADA);
        sesionClaseRepository.saveAndFlush(programada);

        var resultado = sesionClaseRepository.buscarConFiltros(null, null, null, null,
                SesionClaseEstado.ABIERTA);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(SesionClaseEstado.ABIERTA);
    }
}