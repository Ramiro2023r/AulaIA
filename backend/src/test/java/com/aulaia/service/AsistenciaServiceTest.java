package com.aulaia.service;

import com.aulaia.dto.asistencia.RegistrarAsistenciaRequest;
import com.aulaia.dto.asistencia.RegistrarAsistenciaResponse;
import com.aulaia.entity.Asistencia;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.Horario;
import com.aulaia.entity.MetodoRegistro;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.SesionClase;
import com.aulaia.entity.SesionClaseEstado;
import com.aulaia.exception.BusinessException;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.SesionClaseRepository;
import com.aulaia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link AsistenciaService} (Prompt 7.3).
 *
 * <p>Cubre el flujo completo de 9 pasos y sus variantes de error. Todos los
 * colaboradores son mocks. El {@link Clock} es fijo y determinista.
 * Datos ficticios; ningún nombre real de menor.
 */
class AsistenciaServiceTest {

    // =========================================================================
    // Fixtures compartidos
    // =========================================================================

    private SesionClaseRepository sesionClaseRepository;
    private AsistenciaRepository asistenciaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.AuditoriaRepository auditoriaRepository;
    private EstudianteResolverService estudianteResolverService;
    private UsuarioRepository usuarioRepository;
    private DocenteRepository docenteRepository;
    private AuditService auditService;
    private ApplicationEventPublisher eventPublisher;
    private AsistenciaService service;

    /**
     * Reloj fijo: 08:15 (hora local). Los tests usan horaInicio=08:00 y
     * tolerancia=15 min -> límite=08:15 -> justo en el límite -> PRESENTE.
     * Con tolerancia=10 min -> límite=08:10 -> ahora 08:15 -> TARDANZA.
     */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2024-03-01T13:15:00Z"), ZoneId.of("America/Lima"));

    @BeforeEach
    void setUp() {
        sesionClaseRepository = mock(SesionClaseRepository.class);
        asistenciaRepository = mock(AsistenciaRepository.class);
        estudianteResolverService = mock(EstudianteResolverService.class);
        usuarioRepository = mock(UsuarioRepository.class);
        docenteRepository = mock(DocenteRepository.class);
        auditService = mock(AuditService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        
        service = new AsistenciaService(
                asistenciaRepository,
                sesionClaseRepository,
                estudianteResolverService,
                usuarioRepository,
                docenteRepository,
                auditService,
                FIXED_CLOCK,
                eventPublisher);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Seccion seccion(Long id) {
        Seccion s = new Seccion();
        s.setId(id);
        s.setNombre("A");
        return s;
    }

    private Horario horario(Long id, Seccion seccion, LocalTime horaInicio, int tolerancia) {
        Horario h = new Horario();
        h.setId(id);
        h.setSeccion(seccion);
        h.setHoraInicio(horaInicio);
        h.setHoraFin(horaInicio.plusHours(1));
        h.setToleranciaMinutos((short) tolerancia);
        h.setDiaSemana((short) 1);
        return h;
    }

    private SesionClase sesionAbierta(Long id, Horario horario) {
        SesionClase s = new SesionClase();
        s.setId(id);
        s.setHorario(horario);
        s.setEstado(SesionClaseEstado.ABIERTA);
        s.setFecha(java.time.LocalDate.now());
        return s;
    }

    private SesionClase sesion(Long id, Horario horario, SesionClaseEstado estado) {
        SesionClase s = sesionAbierta(id, horario);
        s.setEstado(estado);
        return s;
    }

    private Estudiante estudiante(Long id, Seccion seccion, boolean activo) {
        Estudiante e = new Estudiante();
        e.setId(id);
        e.setCodigo("EST00" + id);
        e.setQrToken("token-" + id);
        e.setNombres("Juan");
        e.setApellidos("Pérez");
        e.setSeccion(seccion);
        e.setActivo(activo);
        return e;
    }

    private RegistrarAsistenciaRequest req(Long sesionId, String codigo, MetodoRegistro metodo) {
        return new RegistrarAsistenciaRequest(codigo, metodo, sesionId);
    }

    // =========================================================================
    // Flujo feliz — PRESENTE
    // =========================================================================

    /**
     * Reloj fijo 08:15. HoraInicio=08:00, tolerancia=15 → límite=08:15.
     * horaActual (08:15) NO es posterior al límite (08:15) → PRESENTE.
     */
    @Test
    void flujoCompleto_devuelvePresenteCuandoHoraEnVentana() {
        Seccion seccion = seccion(10L);
        Horario horario = horario(1L, seccion, LocalTime.of(8, 0), 15);
        SesionClase sesion = sesionAbierta(100L, horario);
        Estudiante est = estudiante(20L, seccion, true);

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(estudianteResolverService.resolver(MetodoRegistro.QR, "AULAIA:STUDENT:tok"))
                .thenReturn(est);
        when(asistenciaRepository.existsBySesionClaseIdAndEstudianteId(100L, 20L)).thenReturn(false);
        when(asistenciaRepository.save(any(Asistencia.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrarAsistenciaResponse resp =
                service.registrar(req(100L, "AULAIA:STUDENT:tok", MetodoRegistro.QR));

        assertThat(resp.success()).isTrue();
        assertThat(resp.estado()).isEqualTo(EstadoAsistencia.PRESENTE);
        assertThat(resp.nombre()).isEqualTo("Juan");
        assertThat(resp.hora()).isNotNull();
        verify(eventPublisher).publishEvent(any(AsistenciaRegistradaEvent.class));
    }

    // =========================================================================
    // Flujo feliz — TARDANZA
    // =========================================================================

    /**
     * Reloj fijo 08:15. HoraInicio=08:00, tolerancia=10 → límite=08:10.
     * horaActual (08:15) es posterior al límite (08:10) → TARDANZA.
     */
    @Test
    void flujoCompleto_devuelveTardanzaCuandoFueraDeVentana() {
        Seccion seccion = seccion(10L);
        Horario horario = horario(1L, seccion, LocalTime.of(8, 0), 10);
        SesionClase sesion = sesionAbierta(100L, horario);
        Estudiante est = estudiante(20L, seccion, true);

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(estudianteResolverService.resolver(MetodoRegistro.CODIGO, "EST0020"))
                .thenReturn(est);
        when(asistenciaRepository.existsBySesionClaseIdAndEstudianteId(100L, 20L)).thenReturn(false);
        when(asistenciaRepository.save(any(Asistencia.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrarAsistenciaResponse resp =
                service.registrar(req(100L, "EST0020", MetodoRegistro.CODIGO));

        assertThat(resp.success()).isTrue();
        assertThat(resp.estado()).isEqualTo(EstadoAsistencia.TARDANZA);
    }

    // =========================================================================
    // Paso 1 — validar sesión
    // =========================================================================

    @Test
    void sesionInexistente_lanzaResourceNotFound() {
        when(sesionClaseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.registrar(req(999L, "AULAIA:STUDENT:tok", MetodoRegistro.QR)))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo(AsistenciaService.CODE_SESSION_NOT_FOUND));
    }

    @Test
    void sesionCerrada_lanzaBusinessExceptionSessionNotActive() {
        Seccion sec = seccion(10L);
        Horario hor = horario(1L, sec, LocalTime.of(8, 0), 15);
        SesionClase sesion = sesion(100L, hor, SesionClaseEstado.CERRADA);

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        assertThatThrownBy(() ->
                service.registrar(req(100L, "AULAIA:STUDENT:tok", MetodoRegistro.QR)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(AsistenciaService.CODE_SESSION_NOT_ACTIVE));
    }

    @Test
    void sesionProgramada_lanzaBusinessExceptionSessionNotActive() {
        Seccion sec = seccion(10L);
        Horario hor = horario(1L, sec, LocalTime.of(8, 0), 15);
        SesionClase sesion = sesion(100L, hor, SesionClaseEstado.PROGRAMADA);

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));

        assertThatThrownBy(() ->
                service.registrar(req(100L, "AULAIA:STUDENT:tok", MetodoRegistro.QR)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(AsistenciaService.CODE_SESSION_NOT_ACTIVE));
    }

    // =========================================================================
    // Paso 3 — validar estudiante activo
    // =========================================================================

    @Test
    void estudianteInactivo_lanzaBusinessException() {
        Seccion sec = seccion(10L);
        Horario hor = horario(1L, sec, LocalTime.of(8, 0), 15);
        SesionClase sesion = sesionAbierta(100L, hor);
        Estudiante est = estudiante(20L, sec, false); // INACTIVO

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(estudianteResolverService.resolver(MetodoRegistro.CODIGO, "EST0020"))
                .thenReturn(est);

        assertThatThrownBy(() ->
                service.registrar(req(100L, "EST0020", MetodoRegistro.CODIGO)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("STUDENT_INACTIVE"));
    }

    // =========================================================================
    // Paso 4 — validar sección
    // =========================================================================

    @Test
    void estudianteDeOtraSeccion_lanzaBusinessExceptionStudentNotInSection() {
        Seccion seccionSesion = seccion(10L);
        Seccion seccionEstudiante = seccion(99L); // diferente
        Horario hor = horario(1L, seccionSesion, LocalTime.of(8, 0), 15);
        SesionClase sesion = sesionAbierta(100L, hor);
        Estudiante est = estudiante(20L, seccionEstudiante, true);

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(estudianteResolverService.resolver(MetodoRegistro.CODIGO, "EST0020"))
                .thenReturn(est);

        assertThatThrownBy(() ->
                service.registrar(req(100L, "EST0020", MetodoRegistro.CODIGO)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(AsistenciaService.CODE_STUDENT_NOT_IN_SECTION));
    }

    // =========================================================================
    // Paso 5 — verificar duplicado
    // =========================================================================

    @Test
    void asistenciaDuplicada_lanzaConflictException() {
        Seccion sec = seccion(10L);
        Horario hor = horario(1L, sec, LocalTime.of(8, 0), 15);
        SesionClase sesion = sesionAbierta(100L, hor);
        Estudiante est = estudiante(20L, sec, true);

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(estudianteResolverService.resolver(MetodoRegistro.CODIGO, "EST0020"))
                .thenReturn(est);
        when(asistenciaRepository.existsBySesionClaseIdAndEstudianteId(100L, 20L))
                .thenReturn(true); // YA EXISTE

        assertThatThrownBy(() ->
                service.registrar(req(100L, "EST0020", MetodoRegistro.CODIGO)))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo(AsistenciaService.CODE_ALREADY_REGISTERED));

        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    void guardar_concurrencia_lanzaConflictException() {
        Seccion sec = seccion(10L);
        Horario hor = horario(1L, sec, LocalTime.of(8, 0), 15);
        SesionClase sesion = sesionAbierta(100L, hor);
        Estudiante est = estudiante(20L, sec, true);

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(estudianteResolverService.resolver(MetodoRegistro.CODIGO, "EST0020")).thenReturn(est);
        // Pasa la validación de duplicado inicial porque la otra transacción aún no ha insertado o commiteado
        when(asistenciaRepository.existsBySesionClaseIdAndEstudianteId(100L, 20L)).thenReturn(false);

        // Pero al guardar la BD lanza violación de constraint
        when(asistenciaRepository.save(any())).thenThrow(
                new DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint \"uq_asistencia_sesion_estudiante\""));

        assertThatThrownBy(() ->
                service.registrar(req(100L, "EST0020", MetodoRegistro.CODIGO)))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo(AsistenciaService.CODE_ALREADY_REGISTERED));
    }

    // =========================================================================
    // Paso 6-7 — hora del servidor y cálculo de estado
    // =========================================================================

    /**
     * Verifica que la hora en la respuesta proviene del Clock inyectado,
     * no del frontend.
     */
    @Test
    void horaEnRespuestaEsDelServidor() {
        Seccion sec = seccion(10L);
        Horario hor = horario(1L, sec, LocalTime.of(8, 0), 15);
        SesionClase sesion = sesionAbierta(100L, hor);
        Estudiante est = estudiante(20L, sec, true);

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(estudianteResolverService.resolver(MetodoRegistro.CODIGO, "EST0020"))
                .thenReturn(est);
        when(asistenciaRepository.existsBySesionClaseIdAndEstudianteId(100L, 20L)).thenReturn(false);
        when(asistenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegistrarAsistenciaResponse resp =
                service.registrar(req(100L, "EST0020", MetodoRegistro.CODIGO));

        OffsetDateTime esperada = OffsetDateTime.now(FIXED_CLOCK);
        assertThat(resp.hora()).isEqualTo(esperada);
    }

    // =========================================================================
    // Paso 8 — guardar: verificar entidad persistida
    // =========================================================================

    @Test
    void asistenciaGuardadaTieneMetodoCorrecto() {
        Seccion sec = seccion(10L);
        Horario hor = horario(1L, sec, LocalTime.of(8, 0), 15);
        SesionClase sesion = sesionAbierta(100L, hor);
        Estudiante est = estudiante(20L, sec, true);

        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesion));
        when(estudianteResolverService.resolver(MetodoRegistro.QR, "AULAIA:STUDENT:tok"))
                .thenReturn(est);
        when(asistenciaRepository.existsBySesionClaseIdAndEstudianteId(100L, 20L)).thenReturn(false);
        when(asistenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.registrar(req(100L, "AULAIA:STUDENT:tok", MetodoRegistro.QR));

        ArgumentCaptor<Asistencia> captor = ArgumentCaptor.forClass(Asistencia.class);
        verify(asistenciaRepository).save(captor.capture());
        Asistencia guardada = captor.getValue();

        assertThat(guardada.getMetodo()).isEqualTo(MetodoRegistro.QR);
        assertThat(guardada.getSesionClase()).isSameAs(sesion);
        assertThat(guardada.getEstudiante()).isSameAs(est);
        assertThat(guardada.getFechaHora()).isNotNull();
    }

    // =========================================================================
    // calcularEstado — pruebas aisladas
    // =========================================================================

    @Test
    void calcularEstado_horaExactaLimite_devuelvePresente() {
        // horaInicio=08:00, tolerancia=15 → límite=08:15
        // FIXED_CLOCK = 08:15 → NO es posterior → PRESENTE
        Seccion sec = seccion(10L);
        Horario hor = horario(1L, sec, LocalTime.of(8, 0), 15);
        SesionClase sesion = sesionAbierta(100L, hor);

        EstadoAsistencia estado = service.calcularEstado(OffsetDateTime.now(FIXED_CLOCK), sesion);
        assertThat(estado).isEqualTo(EstadoAsistencia.PRESENTE);
    }

    @Test
    void calcularEstado_unMinutoDespuesDelLimite_devuelveTardanza() {
        // horaInicio=08:00, tolerancia=14 → límite=08:14
        // FIXED_CLOCK = 08:15 → ES posterior → TARDANZA
        Seccion sec = seccion(10L);
        Horario hor = horario(1L, sec, LocalTime.of(8, 0), 14);
        SesionClase sesion = sesionAbierta(100L, hor);

        EstadoAsistencia estado = service.calcularEstado(OffsetDateTime.now(FIXED_CLOCK), sesion);
        assertThat(estado).isEqualTo(EstadoAsistencia.TARDANZA);
    }

    @Test
    void calcularEstado_antesDelInicio_devuelvePresente() {
        // horaInicio=09:00, tolerancia=15 → límite=09:15
        // FIXED_CLOCK = 08:15 → NO es posterior → PRESENTE
        Seccion sec = seccion(10L);
        Horario hor = horario(1L, sec, LocalTime.of(9, 0), 15);
        SesionClase sesion = sesionAbierta(100L, hor);

        EstadoAsistencia estado = service.calcularEstado(OffsetDateTime.now(FIXED_CLOCK), sesion);
        assertThat(estado).isEqualTo(EstadoAsistencia.PRESENTE);
    }
}
