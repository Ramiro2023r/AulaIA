package com.aulaia.service;

import com.aulaia.dto.horario.HorarioRequest;
import com.aulaia.dto.horario.HorarioResponse;
import com.aulaia.entity.Curso;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Horario;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.BusinessException;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ForbiddenOperationException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.HorarioMapperImpl;
import com.aulaia.repository.CursoRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.HorarioRepository;
import com.aulaia.repository.SeccionRepository;
import com.aulaia.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link HorarioService} (Prompts 5.3 y 5.4), con
 * repositorios mockeados, {@link HorarioMapperImpl} real y validador Bean
 * real. No dependen de PostgreSQL; los casos de borde del solapamiento y
 * las queries reales se cubren contra BD real en
 * {@code HorarioRepositoryTest}. Datos ficticios, nunca reales.
 */
class HorarioServiceTest {

    private HorarioRepository horarioRepository;
    private CursoRepository cursoRepository;
    private SeccionRepository seccionRepository;
    private DocenteRepository docenteRepository;
    private UsuarioRepository usuarioRepository;
    private AuditService auditService;
    private HorarioService horarioService;

    @BeforeEach
    void setUp() {
        horarioRepository = mock(HorarioRepository.class);
        cursoRepository = mock(CursoRepository.class);
        seccionRepository = mock(SeccionRepository.class);
        docenteRepository = mock(DocenteRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        auditService = mock(AuditService.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        horarioService = new HorarioService(horarioRepository, cursoRepository, seccionRepository,
                docenteRepository, usuarioRepository, new HorarioMapperImpl(), validator, auditService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Horario horario(Long id, Long docenteId, Long seccionId, short dia,
                            LocalTime inicio, LocalTime fin) {
        Curso curso = new Curso();
        curso.setId(1L);
        Seccion seccion = new Seccion();
        seccion.setId(seccionId);
        Docente docente = new Docente();
        docente.setId(docenteId);
        Horario horario = new Horario();
        horario.setId(id);
        horario.setCurso(curso);
        horario.setSeccion(seccion);
        horario.setDocente(docente);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        return horario;
    }

    private Horario horarioCompleto(Long id, Long docenteId, String nombresDocente,
                                    Long seccionId, String nombreSeccion,
                                    Long cursoId, String nombreCurso, boolean activo) {
        Curso curso = new Curso();
        curso.setId(cursoId);
        curso.setNombre(nombreCurso);
        Seccion seccion = new Seccion();
        seccion.setId(seccionId);
        seccion.setNombre(nombreSeccion);
        Docente docente = new Docente();
        docente.setId(docenteId);
        docente.setNombres(nombresDocente);
        docente.setApellidos("De Prueba");
        Horario horario = new Horario();
        horario.setId(id);
        horario.setCurso(curso);
        horario.setSeccion(seccion);
        horario.setDocente(docente);
        horario.setDiaSemana((short) 1);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(9, 0));
        horario.setActivo(activo);
        return horario;
    }

    private Usuario usuario(Long id, String username, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }

    private Docente docente(Long id, Long usuarioId) {
        Docente docente = new Docente();
        docente.setId(id);
        docente.setUsuario(usuario(usuarioId, "doc." + id, Rol.DOCENTE));
        docente.setNombres("Docente");
        docente.setApellidos("De Prueba");
        return docente;
    }

    private Curso curso(Long id) {
        Curso curso = new Curso();
        curso.setId(id);
        curso.setNombre("Matemática");
        return curso;
    }

    private Seccion seccion(Long id) {
        Seccion seccion = new Seccion();
        seccion.setId(id);
        seccion.setNombre("A");
        return seccion;
    }

    private HorarioRequest request(Long cursoId, Long seccionId, Long docenteId,
                                   short dia, LocalTime inicio, LocalTime fin) {
        return new HorarioRequest(cursoId, seccionId, docenteId, dia, inicio, fin, (short) 10, (short) 15);
    }

    private void stubRelacionesExistentes() {
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso(1L)));
        when(seccionRepository.findById(20L)).thenReturn(Optional.of(seccion(20L)));
        when(docenteRepository.findById(10L)).thenReturn(Optional.of(docente(10L, 2L)));
    }

    private void sinConflictos() {
        when(horarioRepository.existeConflictoDocente(any(), any(), any(), any(), any())).thenReturn(false);
        when(horarioRepository.existeConflictoSeccion(any(), any(), any(), any(), any())).thenReturn(false);
    }

    private void autenticar(String username, Long usuarioId, Rol rol) {
        UserDetails principal = User.withUsername(username).password("x").authorities("ROLE_" + rol.name()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuario(usuarioId, username, rol)));
    }

    // ============ Prompt 5.3: validación de conflictos ============

    @Test
    void horarioSinConflictoEsValido() {
        sinConflictos();

        assertThatCode(() -> horarioService.validarConflictos(
                horario(null, 10L, 20L, (short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .doesNotThrowAnyException();
    }

    @Test
    void mismoDocenteSolapadoGeneraConflictoDocente() {
        when(horarioRepository.existeConflictoDocente(10L, (short) 1,
                LocalTime.of(9, 0), LocalTime.of(11, 0), null)).thenReturn(true);

        assertThatThrownBy(() -> horarioService.validarConflictos(
                horario(null, 10L, 20L, (short) 1, LocalTime.of(9, 0), LocalTime.of(11, 0))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("docente")
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("TEACHER_SCHEDULE_CONFLICT"));
    }

    @Test
    void horariosConsecutivosDelMismoDocenteSonValidos() {
        sinConflictos();

        assertThatCode(() -> horarioService.validarConflictos(
                horario(null, 10L, 20L, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0))))
                .doesNotThrowAnyException();
    }

    @Test
    void mismoDocenteDistintoDiaNoGeneraConflicto() {
        sinConflictos();

        assertThatCode(() -> horarioService.validarConflictos(
                horario(null, 10L, 20L, (short) 2, LocalTime.of(9, 0), LocalTime.of(11, 0))))
                .doesNotThrowAnyException();
    }

    @Test
    void mismoDocenteIntervaloAnteriorSinCruceEsValido() {
        sinConflictos();

        assertThatCode(() -> horarioService.validarConflictos(
                horario(null, 10L, 20L, (short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .doesNotThrowAnyException();
    }

    @Test
    void mismoDocenteIntervaloPosteriorSinCruceEsValido() {
        sinConflictos();

        assertThatCode(() -> horarioService.validarConflictos(
                horario(null, 10L, 20L, (short) 1, LocalTime.of(10, 0), LocalTime.of(11, 0))))
                .doesNotThrowAnyException();
    }

    @Test
    void mismaSeccionSolapadaGeneraConflictoSeccion() {
        when(horarioRepository.existeConflictoSeccion(20L, (short) 1,
                LocalTime.of(9, 0), LocalTime.of(11, 0), null)).thenReturn(true);

        assertThatThrownBy(() -> horarioService.validarConflictos(
                horario(null, 10L, 20L, (short) 1, LocalTime.of(9, 0), LocalTime.of(11, 0))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("sección")
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SECTION_SCHEDULE_CONFLICT"));
    }

    @Test
    void mismaSeccionConsecutivaEsValida() {
        sinConflictos();

        assertThatCode(() -> horarioService.validarConflictos(
                horario(null, 10L, 20L, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0))))
                .doesNotThrowAnyException();
    }

    @Test
    void mismaSeccionDistintoDiaNoGeneraConflicto() {
        sinConflictos();

        assertThatCode(() -> horarioService.validarConflictos(
                horario(null, 10L, 20L, (short) 3, LocalTime.of(9, 0), LocalTime.of(11, 0))))
                .doesNotThrowAnyException();
    }

    @Test
    void mismoCursoConDocenteYSeccionDiferentesNoGeneraConflicto() {
        sinConflictos();

        assertThatCode(() -> horarioService.validarConflictos(
                horario(null, 99L, 98L, (short) 1, LocalTime.of(9, 0), LocalTime.of(11, 0))))
                .doesNotThrowAnyException();
    }

    @Test
    void actualizarSinCambiarIntervaloNoEntraEnConflictoConsigoMismo() {
        sinConflictos();

        Horario existente = horario(55L, 10L, 20L, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertThatCode(() -> horarioService.validarConflictos(existente)).doesNotThrowAnyException();

        verify(horarioRepository).existeConflictoDocente(10L, (short) 1,
                LocalTime.of(9, 0), LocalTime.of(10, 0), 55L);
        verify(horarioRepository).existeConflictoSeccion(20L, (short) 1,
                LocalTime.of(9, 0), LocalTime.of(10, 0), 55L);
    }

    @Test
    void actualizarHaciaIntervaloConflictivoDeDocenteGeneraConflicto() {
        when(horarioRepository.existeConflictoDocente(10L, (short) 1,
                LocalTime.of(9, 0), LocalTime.of(11, 0), 55L)).thenReturn(true);

        assertThatThrownBy(() -> horarioService.validarConflictos(
                horario(55L, 10L, 20L, (short) 1, LocalTime.of(9, 0), LocalTime.of(11, 0))))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("TEACHER_SCHEDULE_CONFLICT"));
    }

    @Test
    void actualizarHaciaIntervaloConflictivoDeSeccionGeneraConflicto() {
        when(horarioRepository.existeConflictoSeccion(20L, (short) 1,
                LocalTime.of(9, 0), LocalTime.of(11, 0), 55L)).thenReturn(true);

        assertThatThrownBy(() -> horarioService.validarConflictos(
                horario(55L, 10L, 20L, (short) 1, LocalTime.of(9, 0), LocalTime.of(11, 0))))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SECTION_SCHEDULE_CONFLICT"));
    }

    @Test
    void horarioIndividualInvalidoSeRechazaAntesQueConflicto() {
        when(horarioRepository.existeConflictoDocente(any(), any(), any(), any(), any())).thenReturn(true);
        when(horarioRepository.existeConflictoSeccion(any(), any(), any(), any(), any())).thenReturn(true);

        Horario invalido = horario(null, 10L, 20L, (short) 1, LocalTime.of(10, 0), LocalTime.of(9, 0));

        assertThatThrownBy(() -> horarioService.validarConflictos(invalido))
                .isInstanceOf(ConstraintViolationException.class);
        verify(horarioRepository, never()).existeConflictoDocente(any(), any(), any(), any(), any());
    }

    @Test
    void conflictoDobleReportaDocentePrimero() {
        when(horarioRepository.existeConflictoDocente(10L, (short) 1,
                LocalTime.of(9, 0), LocalTime.of(11, 0), null)).thenReturn(true);

        assertThatThrownBy(() -> horarioService.validarConflictos(
                horario(null, 10L, 20L, (short) 1, LocalTime.of(9, 0), LocalTime.of(11, 0))))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("TEACHER_SCHEDULE_CONFLICT"));
    }

    @Test
    void diaFueraDeRangoSeRechazaAntesQueConflicto() {
        when(horarioRepository.existeConflictoDocente(any(), any(), any(), any(), any())).thenReturn(true);

        Horario invalido = horario(null, 10L, 20L, (short) 8, LocalTime.of(8, 0), LocalTime.of(9, 0));

        assertThatThrownBy(() -> horarioService.validarConflictos(invalido))
                .isInstanceOf(ConstraintViolationException.class);
        verify(horarioRepository, never()).existeConflictoDocente(any(), any(), any(), any(), any());
    }

    // ============ Prompt 5.4: CRUD ============

    @Test
    void crearHorarioCorrecto() {
        stubRelacionesExistentes();
        sinConflictos();
        when(horarioRepository.saveAndFlush(any(Horario.class))).thenAnswer(inv -> {
            Horario h = inv.getArgument(0);
            h.setId(77L);
            return h;
        });

        HorarioResponse response = horarioService.crear(
                request(1L, 20L, 10L, (short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0)));

        assertThat(response.id()).isEqualTo(77L);
        assertThat(response.diaSemana()).isEqualTo((short) 1);
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.activo()).isTrue();
        assertThat(response.curso().nombre()).isEqualTo("Matemática");
        assertThat(response.seccion().nombre()).isEqualTo("A");
        assertThat(response.docente().id()).isEqualTo(10L);
        verify(horarioRepository).saveAndFlush(any(Horario.class));
    }

    @Test
    void crearCursoInexistenteLanzaCourseNotFound() {
        stubRelacionesExistentes();
        when(cursoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> horarioService.crear(
                request(1L, 20L, 10L, (short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo("COURSE_NOT_FOUND"));
    }

    @Test
    void crearSeccionInexistenteLanzaSectionNotFound() {
        stubRelacionesExistentes();
        when(seccionRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> horarioService.crear(
                request(1L, 20L, 10L, (short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo("SECTION_NOT_FOUND"));
    }

    @Test
    void crearDocenteInexistenteLanzaTeacherNotFound() {
        stubRelacionesExistentes();
        when(docenteRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> horarioService.crear(
                request(1L, 20L, 10L, (short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo("TEACHER_NOT_FOUND"));
    }

    @Test
    void crearConConflictoDocenteLanza409() {
        stubRelacionesExistentes();
        when(horarioRepository.existeConflictoDocente(10L, (short) 1,
                LocalTime.of(8, 0), LocalTime.of(9, 0), null)).thenReturn(true);

        assertThatThrownBy(() -> horarioService.crear(
                request(1L, 20L, 10L, (short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("TEACHER_SCHEDULE_CONFLICT"));
    }

    @Test
    void crearConConflictoSeccionLanza409() {
        stubRelacionesExistentes();
        when(horarioRepository.existeConflictoSeccion(20L, (short) 1,
                LocalTime.of(8, 0), LocalTime.of(9, 0), null)).thenReturn(true);

        assertThatThrownBy(() -> horarioService.crear(
                request(1L, 20L, 10L, (short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SECTION_SCHEDULE_CONFLICT"));
    }

    @Test
    void crearConsecutivoValidoGuarda() {
        stubRelacionesExistentes();
        sinConflictos();
        when(horarioRepository.saveAndFlush(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));

        HorarioResponse response = horarioService.crear(
                request(1L, 20L, 10L, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(9, 0));
        verify(horarioRepository).saveAndFlush(any(Horario.class));
    }

    @Test
    void buscarHorarioExistente() {
        when(horarioRepository.findById(5L)).thenReturn(Optional.of(
                horarioCompleto(5L, 10L, "Docente", 20L, "A", 1L, "Matemática", true)));

        HorarioResponse response = horarioService.buscarHorario(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.docente().nombres()).isEqualTo("Docente");
        assertThat(response.curso().nombre()).isEqualTo("Matemática");
    }

    @Test
    void buscarHorarioInexistenteLanzaScheduleNotFound() {
        when(horarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> horarioService.buscarHorario(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo("SCHEDULE_NOT_FOUND"));
    }

    @Test
    void actualizarHorarioCorrectoConservaActivoYExcluyePropioId() {
        stubRelacionesExistentes();
        sinConflictos();
        Horario existente = horarioCompleto(5L, 10L, "Docente", 20L, "A", 1L, "Matemática", false);
        when(horarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(horarioRepository.saveAndFlush(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));

        HorarioResponse response = horarioService.actualizar(5L,
                request(1L, 20L, 10L, (short) 3, LocalTime.of(11, 0), LocalTime.of(12, 0)));

        assertThat(response.diaSemana()).isEqualTo((short) 3);
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(11, 0));
        assertThat(response.activo()).isFalse();
        verify(horarioRepository).existeConflictoDocente(10L, (short) 3,
                LocalTime.of(11, 0), LocalTime.of(12, 0), 5L);
        verify(horarioRepository).existeConflictoSeccion(20L, (short) 3,
                LocalTime.of(11, 0), LocalTime.of(12, 0), 5L);
    }

    @Test
    void actualizarHorarioInexistenteLanzaScheduleNotFound() {
        when(horarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> horarioService.actualizar(99L,
                request(1L, 20L, 10L, (short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo("SCHEDULE_NOT_FOUND"));
    }

    @Test
    void actualizarHaciaConflictoDocenteLanza409() {
        stubRelacionesExistentes();
        when(horarioRepository.findById(5L)).thenReturn(Optional.of(
                horarioCompleto(5L, 10L, "Docente", 20L, "A", 1L, "Matemática", true)));
        when(horarioRepository.existeConflictoDocente(10L, (short) 1,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 5L)).thenReturn(true);

        assertThatThrownBy(() -> horarioService.actualizar(5L,
                request(1L, 20L, 10L, (short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("TEACHER_SCHEDULE_CONFLICT"));
    }

    @Test
    void actualizarHaciaConflictoSeccionLanza409() {
        stubRelacionesExistentes();
        when(horarioRepository.findById(5L)).thenReturn(Optional.of(
                horarioCompleto(5L, 10L, "Docente", 20L, "A", 1L, "Matemática", true)));
        when(horarioRepository.existeConflictoSeccion(20L, (short) 1,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 5L)).thenReturn(true);

        assertThatThrownBy(() -> horarioService.actualizar(5L,
                request(1L, 20L, 10L, (short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SECTION_SCHEDULE_CONFLICT"));
    }

    // ============ Prompt 5.4: filtros ============

    private List<Horario> dosHorarios() {
        return List.of(
                horarioCompleto(1L, 10L, "Docente", 20L, "A", 1L, "Matemática", true),
                horarioCompleto(2L, 11L, "Otro", 21L, "B", 2L, "Ciencias", true));
    }

    @Test
    void listarFiltraPorDocente() {
        when(horarioRepository.buscarConFiltros(10L, null, null, null)).thenReturn(dosHorarios());

        List<HorarioResponse> lista = horarioService.listarHorarios(10L, null, null, null);

        assertThat(lista).hasSize(2);
        verify(horarioRepository).buscarConFiltros(10L, null, null, null);
    }

    @Test
    void listarFiltraPorSeccion() {
        when(horarioRepository.buscarConFiltros(null, 20L, null, null)).thenReturn(dosHorarios());

        horarioService.listarHorarios(null, 20L, null, null);

        verify(horarioRepository).buscarConFiltros(null, 20L, null, null);
    }

    @Test
    void listarFiltraPorCurso() {
        when(horarioRepository.buscarConFiltros(null, null, 3L, null)).thenReturn(dosHorarios());

        horarioService.listarHorarios(null, null, 3L, null);

        verify(horarioRepository).buscarConFiltros(null, null, 3L, null);
    }

    @Test
    void listarFiltraPorDia() {
        when(horarioRepository.buscarConFiltros(null, null, null, (short) 5)).thenReturn(dosHorarios());

        horarioService.listarHorarios(null, null, null, (short) 5);

        verify(horarioRepository).buscarConFiltros(null, null, null, (short) 5);
    }

    @Test
    void listarFiltroDiaFueraDeRangoLanzaValidacion() {
        assertThatThrownBy(() -> horarioService.listarHorarios(null, null, null, (short) 8))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("VALIDATION_ERROR"));
        verify(horarioRepository, never()).buscarConFiltros(any(), any(), any(), any());
    }

    @Test
    void listarCombinaFiltrosConAnd() {
        when(horarioRepository.buscarConFiltros(10L, 20L, 3L, (short) 2)).thenReturn(dosHorarios());

        horarioService.listarHorarios(10L, 20L, 3L, (short) 2);

        verify(horarioRepository).buscarConFiltros(10L, 20L, 3L, (short) 2);
    }

    // ============ Prompt 5.4: autorización contextual ============

    @Test
    void docenteListaSoloSusHorariosEIgnoraDocenteIdAjeno() {
        autenticar("doc.a", 2L, Rol.DOCENTE);
        when(docenteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(docente(20L, 2L)));
        when(horarioRepository.buscarConFiltros(20L, null, null, null)).thenReturn(dosHorarios());

        List<HorarioResponse> lista = horarioService.listarHorarios(999L, null, null, null);

        assertThat(lista).hasSize(2);
        verify(horarioRepository).buscarConFiltros(20L, null, null, null);
    }

    @Test
    void docenteIntentaVerHorarioAjenoLanzaForbidden() {
        autenticar("doc.a", 2L, Rol.DOCENTE);
        when(docenteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(docente(20L, 2L)));
        when(horarioRepository.findById(9L)).thenReturn(Optional.of(
                horarioCompleto(9L, 21L, "Otro", 21L, "B", 2L, "Ciencias", true)));

        assertThatThrownBy(() -> horarioService.buscarHorario(9L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void docenteVeSuPropioHorario() {
        autenticar("doc.a", 2L, Rol.DOCENTE);
        when(docenteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(docente(20L, 2L)));
        when(horarioRepository.findById(9L)).thenReturn(Optional.of(
                horarioCompleto(9L, 20L, "Docente", 21L, "B", 2L, "Ciencias", true)));

        HorarioResponse response = horarioService.buscarHorario(9L);

        assertThat(response.id()).isEqualTo(9L);
    }

    @Test
    void adminPuedeVerHorarioDeCualquierDocente() {
        autenticar("admin", 1L, Rol.ADMIN);
        when(horarioRepository.findById(9L)).thenReturn(Optional.of(
                horarioCompleto(9L, 21L, "Otro", 21L, "B", 2L, "Ciencias", true)));

        HorarioResponse response = horarioService.buscarHorario(9L);

        assertThat(response.id()).isEqualTo(9L);
    }

    @Test
    void adminListaConFiltroDeDocenteSinRestriccion() {
        autenticar("admin", 1L, Rol.ADMIN);
        when(horarioRepository.buscarConFiltros(21L, null, null, null)).thenReturn(dosHorarios());

        horarioService.listarHorarios(21L, null, null, null);

        verify(horarioRepository).buscarConFiltros(eq(21L), any(), any(), any());
    }
}