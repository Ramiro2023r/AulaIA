package com.aulaia.repository;

import com.aulaia.entity.Curso;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Grado;
import com.aulaia.entity.Horario;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración de {@link HorarioRepository} contra PostgreSQL
 * real (perfil {@code itest}), siguiendo el patrón del proyecto.
 *
 * <p>Deshabilitadas por defecto. Ejecución explícita (PowerShell):
 * <pre>
 * $env:DB_HOST="localhost"; $env:DB_PORT="5433"; $env:DB_NAME="aulaia_db"
 * $env:DB_USERNAME="aulaia_user"; $env:DB_PASSWORD="TU_PASSWORD_LOCAL"
 * .\mvnw.cmd test "-Dtest=HorarioRepositoryTest" "-Daulaia.itest=true"
 * </pre>
 *
 * <p>Transaccionales: cada test revierte sus cambios. Verifican las
 * restricciones físicas de {@code horarios} (04-BD §6.7): FK a cursos,
 * secciones y docentes; los 4 CHECK documentados; defaults; timestamps.
 * Desde el Prompt 5.3 también verifican las consultas de conflicto de
 * docente y sección (07-PLAN 5.3) y los casos de borde del solapamiento.
 * Datos ficticios, nunca reales.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("itest")
@EnabledIfSystemProperty(named = "aulaia.itest", matches = "true")
class HorarioRepositoryTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static int contadorUsuarios;

    private Curso guardarCurso() {
        Curso curso = new Curso();
        curso.setNombre("Matemática");
        return cursoRepository.saveAndFlush(curso);
    }

    private Seccion guardarSeccion() {
        Grado grado = new Grado();
        grado.setNombre("6.º Primaria");
        grado = gradoRepository.saveAndFlush(grado);
        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setNombre("A");
        seccion.setPeriodoAcademico("2026");
        return seccionRepository.saveAndFlush(seccion);
    }

    private Docente guardarDocente() {
        Usuario usuario = new Usuario();
        usuario.setUsername("doc.horario.test" + (++contadorUsuarios));
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

    private Horario horario(Curso curso, Seccion seccion, Docente docente) {
        Horario horario = new Horario();
        horario.setCurso(curso);
        horario.setSeccion(seccion);
        horario.setDocente(docente);
        horario.setDiaSemana((short) 1);
        horario.setHoraInicio(LocalTime.of(9, 0));
        horario.setHoraFin(LocalTime.of(10, 30));
        return horario;
    }

    @Test
    void guardarHorarioValido() {
        Horario guardado = horarioRepository.saveAndFlush(
                horario(guardarCurso(), guardarSeccion(), guardarDocente()));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getDiaSemana()).isEqualTo((short) 1);
        assertThat(guardado.getHoraInicio()).isEqualTo(LocalTime.of(9, 0));
        assertThat(guardado.getHoraFin()).isEqualTo(LocalTime.of(10, 30));
        assertThat(guardado.getToleranciaMinutos()).isEqualTo((short) 10);
        assertThat(guardado.getMinutosAntesApertura()).isEqualTo((short) 15);
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getCurso().getId()).isNotNull();
        assertThat(guardado.getSeccion().getId()).isNotNull();
        assertThat(guardado.getDocente().getId()).isNotNull();
        assertThat(guardado.getCreatedAt()).isNotNull();
        assertThat(guardado.getUpdatedAt()).isNotNull();
    }

    @Test
    void buscarHorarioPorId() {
        Horario guardado = horarioRepository.saveAndFlush(
                horario(guardarCurso(), guardarSeccion(), guardarDocente()));

        Optional<Horario> encontrado = horarioRepository.findById(guardado.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getDiaSemana()).isEqualTo((short) 1);
        assertThat(encontrado.get().getCurso().getNombre()).isEqualTo("Matemática");
    }

    @Test
    void cursoInexistenteEsRechazado() {
        Curso cursoFantasma = new Curso();
        cursoFantasma.setId(999999L);
        Horario horario = horario(cursoFantasma, guardarSeccion(), guardarDocente());

        assertThatThrownBy(() -> horarioRepository.saveAndFlush(horario))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void seccionInexistenteEsRechazada() {
        Seccion seccionFantasma = new Seccion();
        seccionFantasma.setId(999999L);
        Horario horario = horario(guardarCurso(), seccionFantasma, guardarDocente());

        assertThatThrownBy(() -> horarioRepository.saveAndFlush(horario))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void docenteInexistenteEsRechazado() {
        Docente docenteFantasma = new Docente();
        docenteFantasma.setId(999999L);
        Horario horario = horario(guardarCurso(), guardarSeccion(), docenteFantasma);

        assertThatThrownBy(() -> horarioRepository.saveAndFlush(horario))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void defaultsDeBaseDeDatos() {
        Curso curso = guardarCurso();
        Seccion seccion = guardarSeccion();
        Docente docente = guardarDocente();

        entityManager.createNativeQuery("""
                        INSERT INTO horarios (curso_id, seccion_id, docente_id, dia_semana, hora_inicio, hora_fin)
                        VALUES (:curso, :seccion, :docente, 2, '11:00:00', '12:00:00')""")
                .setParameter("curso", curso.getId())
                .setParameter("seccion", seccion.getId())
                .setParameter("docente", docente.getId())
                .executeUpdate();

        Object[] fila = (Object[]) entityManager.createNativeQuery("""
                        SELECT tolerancia_minutos, minutos_antes_apertura, activo
                        FROM horarios WHERE dia_semana = 2 AND curso_id = :curso""")
                .setParameter("curso", curso.getId())
                .getSingleResult();

        assertThat(((Number) fila[0]).shortValue()).isEqualTo((short) 10);
        assertThat(((Number) fila[1]).shortValue()).isEqualTo((short) 15);
        assertThat((Boolean) fila[2]).isTrue();
    }

    /**
     * Los CHECK físicos (04-BD §6.7) se prueban con SQL nativo: la
     * validación Bean (callback de Hibernate) intercepta antes los valores
     * inválidos en la ruta JPA; el SQL directo llega a la BD y demuestra
     * que la constraint física rechaza (barrera final documentada).
     */
    @Test
    void checkDiaSemanaRechazaFueraDeRango() {
        Curso curso = guardarCurso();
        Seccion seccion = guardarSeccion();
        Docente docente = guardarDocente();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        INSERT INTO horarios (curso_id, seccion_id, docente_id, dia_semana, hora_inicio, hora_fin)
                        VALUES (?, ?, ?, 0, '09:00:00', '10:00:00')""",
                curso.getId(), seccion.getId(), docente.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkToleranciaRechazaNegativa() {
        Curso curso = guardarCurso();
        Seccion seccion = guardarSeccion();
        Docente docente = guardarDocente();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        INSERT INTO horarios (curso_id, seccion_id, docente_id, dia_semana, hora_inicio, hora_fin, tolerancia_minutos)
                        VALUES (?, ?, ?, 1, '09:00:00', '10:00:00', -1)""",
                curso.getId(), seccion.getId(), docente.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkAperturaRechazaNegativa() {
        Curso curso = guardarCurso();
        Seccion seccion = guardarSeccion();
        Docente docente = guardarDocente();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        INSERT INTO horarios (curso_id, seccion_id, docente_id, dia_semana, hora_inicio, hora_fin, minutos_antes_apertura)
                        VALUES (?, ?, ?, 1, '09:00:00', '10:00:00', -1)""",
                curso.getId(), seccion.getId(), docente.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkHorasRechazaHoraFinIgualOAnterior() {
        Curso curso = guardarCurso();
        Seccion seccion = guardarSeccion();
        Docente docente = guardarDocente();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        INSERT INTO horarios (curso_id, seccion_id, docente_id, dia_semana, hora_inicio, hora_fin)
                        VALUES (?, ?, ?, 1, '09:00:00', '09:00:00')""",
                curso.getId(), seccion.getId(), docente.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listarHorariosOrdenadosPorId() {
        horarioRepository.saveAndFlush(horario(guardarCurso(), guardarSeccion(), guardarDocente()));
        horarioRepository.saveAndFlush(horario(guardarCurso(), guardarSeccion(), guardarDocente()));

        List<Horario> lista = horarioRepository.findAllByOrderByIdAsc();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getId()).isLessThan(lista.get(1).getId());
    }

    /**
     * Prompt 5.3: detección de conflictos (07-PLAN 5.3) contra PostgreSQL
     * real. Se persiste un horario A (lunes 09:00-10:00) y se evalúan las
     * consultas con los parámetros de un hipotético horario B (nuevo o
     * actualizado), sin guardarlo.
     */
    @Test
    void conflictoDocenteOverlapEsDetectado() {
        Horario a = horarioRepository.saveAndFlush(
                horario(guardarCurso(), guardarSeccion(), guardarDocente()));

        assertThat(horarioRepository.existeConflictoDocente(
                a.getDocente().getId(), (short) 1, LocalTime.of(9, 30), LocalTime.of(10, 30), null))
                .isTrue();
    }

    @Test
    void conflictoDocenteConsecutivoNoEsDetectado() {
        Horario a = horarioRepository.saveAndFlush(
                horario(guardarCurso(), guardarSeccion(), guardarDocente()));

        assertThat(horarioRepository.existeConflictoDocente(
                a.getDocente().getId(), (short) 1, LocalTime.of(10, 30), LocalTime.of(11, 30), null))
                .isFalse();
    }

    @Test
    void conflictoDocenteDistintoDiaNoEsDetectado() {
        Horario a = horarioRepository.saveAndFlush(
                horario(guardarCurso(), guardarSeccion(), guardarDocente()));

        assertThat(horarioRepository.existeConflictoDocente(
                a.getDocente().getId(), (short) 2, LocalTime.of(9, 30), LocalTime.of(10, 30), null))
                .isFalse();
    }

    @Test
    void actualizarExcluyeElPropioHorario() {
        Horario a = horarioRepository.saveAndFlush(
                horario(guardarCurso(), guardarSeccion(), guardarDocente()));

        assertThat(horarioRepository.existeConflictoDocente(
                a.getDocente().getId(), (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0), a.getId()))
                .isFalse();
        assertThat(horarioRepository.existeConflictoSeccion(
                a.getSeccion().getId(), (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0), a.getId()))
                .isFalse();
    }

    @Test
    void conflictoSeccionOverlapEsDetectado() {
        Horario a = horarioRepository.saveAndFlush(
                horario(guardarCurso(), guardarSeccion(), guardarDocente()));

        assertThat(horarioRepository.existeConflictoSeccion(
                a.getSeccion().getId(), (short) 1, LocalTime.of(9, 30), LocalTime.of(10, 30), null))
                .isTrue();
    }

    @Test
    void conflictoSeccionConsecutivaNoEsDetectada() {
        Horario a = horarioRepository.saveAndFlush(
                horario(guardarCurso(), guardarSeccion(), guardarDocente()));

        assertThat(horarioRepository.existeConflictoSeccion(
                a.getSeccion().getId(), (short) 1, LocalTime.of(10, 30), LocalTime.of(11, 30), null))
                .isFalse();
    }

    @Test
    void conflictoSeccionDistintoDiaNoEsDetectado() {
        Horario a = horarioRepository.saveAndFlush(
                horario(guardarCurso(), guardarSeccion(), guardarDocente()));

        assertThat(horarioRepository.existeConflictoSeccion(
                a.getSeccion().getId(), (short) 4, LocalTime.of(9, 30), LocalTime.of(10, 30), null))
                .isFalse();
    }

    @Test
    void horarioInactivoNoGeneraConflicto() {
        Horario a = horarioRepository.saveAndFlush(
                horario(guardarCurso(), guardarSeccion(), guardarDocente()));
        Horario inactivo = horario(guardarCurso(), guardarSeccion(), guardarDocente());
        inactivo.setDiaSemana((short) 1);
        inactivo.setHoraInicio(LocalTime.of(11, 0));
        inactivo.setHoraFin(LocalTime.of(12, 0));
        inactivo.setActivo(false);
        horarioRepository.saveAndFlush(inactivo);

        assertThat(horarioRepository.existeConflictoDocente(
                inactivo.getDocente().getId(), (short) 1, LocalTime.of(11, 30), LocalTime.of(12, 30), null))
                .isFalse();
        assertThat(horarioRepository.existeConflictoSeccion(
                a.getSeccion().getId(), (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0), null))
                .isTrue();
    }

    @Test
    void bordesDeSolapamientoParaFranjaDeOchoANueve() {
        Horario a = horarioRepository.saveAndFlush(
                horario(guardarCurso(), guardarSeccion(), guardarDocente()));
        a.setDiaSemana((short) 1);
        a.setHoraInicio(LocalTime.of(8, 0));
        a.setHoraFin(LocalTime.of(9, 0));
        horarioRepository.saveAndFlush(a);
        Long docente = a.getDocente().getId();

        assertThat(horarioRepository.existeConflictoDocente(docente, (short) 1,
                LocalTime.of(8, 0), LocalTime.of(9, 0), null)).isTrue();
        assertThat(horarioRepository.existeConflictoDocente(docente, (short) 1,
                LocalTime.of(8, 30), LocalTime.of(8, 45), null)).isTrue();
        assertThat(horarioRepository.existeConflictoDocente(docente, (short) 1,
                LocalTime.of(7, 0), LocalTime.of(8, 0), null)).isFalse();
        assertThat(horarioRepository.existeConflictoDocente(docente, (short) 1,
                LocalTime.of(9, 0), LocalTime.of(10, 0), null)).isFalse();
        assertThat(horarioRepository.existeConflictoDocente(docente, (short) 1,
                LocalTime.of(7, 30), LocalTime.of(8, 30), null)).isTrue();
        assertThat(horarioRepository.existeConflictoDocente(docente, (short) 1,
                LocalTime.of(8, 30), LocalTime.of(9, 30), null)).isTrue();
    }

    // ============ Prompt 5.4: query de filtros (buscarConFiltros) ============

    private Horario horarioCon(Curso curso, Seccion seccion, Docente docente,
                               short dia, LocalTime inicio, LocalTime fin) {
        Horario horario = horario(curso, seccion, docente);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        return horario;
    }

    @Test
    void filtrarPorDocente() {
        Docente docenteA = guardarDocente();
        Docente docenteB = guardarDocente();
        horarioRepository.saveAndFlush(horario(guardarCurso(), guardarSeccion(), docenteA));
        horarioRepository.saveAndFlush(horario(guardarCurso(), guardarSeccion(), docenteB));

        List<Horario> lista = horarioRepository.buscarConFiltros(docenteA.getId(), null, null, null);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getDocente().getId()).isEqualTo(docenteA.getId());
    }

    @Test
    void filtrarPorSeccion() {
        Seccion seccionA = guardarSeccion();
        Seccion seccionB = guardarSeccion();
        horarioRepository.saveAndFlush(horario(guardarCurso(), seccionA, guardarDocente()));
        horarioRepository.saveAndFlush(horario(guardarCurso(), seccionB, guardarDocente()));

        List<Horario> lista = horarioRepository.buscarConFiltros(null, seccionA.getId(), null, null);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getSeccion().getId()).isEqualTo(seccionA.getId());
    }

    @Test
    void filtrarPorCurso() {
        Curso cursoA = guardarCurso();
        Curso cursoB = guardarCurso();
        horarioRepository.saveAndFlush(horario(cursoA, guardarSeccion(), guardarDocente()));
        horarioRepository.saveAndFlush(horario(cursoB, guardarSeccion(), guardarDocente()));

        List<Horario> lista = horarioRepository.buscarConFiltros(null, null, cursoA.getId(), null);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getCurso().getId()).isEqualTo(cursoA.getId());
    }

    @Test
    void filtrarPorDia() {
        horarioRepository.saveAndFlush(horarioCon(guardarCurso(), guardarSeccion(), guardarDocente(),
                (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        horarioRepository.saveAndFlush(horarioCon(guardarCurso(), guardarSeccion(), guardarDocente(),
                (short) 2, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        List<Horario> lista = horarioRepository.buscarConFiltros(null, null, null, (short) 2);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getDiaSemana()).isEqualTo((short) 2);
    }

    @Test
    void combinarFiltrosConAnd() {
        Curso curso = guardarCurso();
        Seccion seccion = guardarSeccion();
        Docente docente = guardarDocente();
        horarioRepository.saveAndFlush(horarioCon(curso, seccion, docente,
                (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        horarioRepository.saveAndFlush(horarioCon(curso, seccion, docente,
                (short) 2, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        List<Horario> lista = horarioRepository.buscarConFiltros(
                docente.getId(), seccion.getId(), curso.getId(), (short) 1);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getDiaSemana()).isEqualTo((short) 1);
    }

    @Test
    void sinFiltrosDevuelveTodosOrdenadosPorId() {
        horarioRepository.saveAndFlush(horarioCon(guardarCurso(), guardarSeccion(), guardarDocente(),
                (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        horarioRepository.saveAndFlush(horarioCon(guardarCurso(), guardarSeccion(), guardarDocente(),
                (short) 3, LocalTime.of(11, 0), LocalTime.of(12, 0)));

        List<Horario> lista = horarioRepository.buscarConFiltros(null, null, null, null);

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getId()).isLessThan(lista.get(1).getId());
    }
}