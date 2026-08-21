package com.aulaia.service;

import com.aulaia.dto.asistencia.AsistenciaCorreccionRequest;
import com.aulaia.dto.asistencia.AsistenciaResponse;
import com.aulaia.dto.asistencia.RegistrarAsistenciaRequest;
import com.aulaia.dto.asistencia.RegistrarAsistenciaResponse;
import com.aulaia.entity.Asistencia;
import com.aulaia.entity.Docente;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.Horario;
import com.aulaia.entity.MetodoRegistro;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.SesionClase;
import com.aulaia.entity.SesionClaseEstado;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.BusinessException;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ForbiddenOperationException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.SesionClaseRepository;
import com.aulaia.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Reglas de negocio para el registro de asistencia (Prompt 7.3 — 07-PLAN Sprint 7).
 *
 * <h3>Flujo obligatorio (07-PLAN 7.3):</h3>
 * <ol>
 *   <li>Validar sesión ABIERTA ({@link SesionClaseEstado#ABIERTA}).</li>
 *   <li>Resolver estudiante por QR o código (delega a
 *       {@link EstudianteResolverService}).</li>
 *   <li>Validar estudiante activo ({@code activo == true}).</li>
 *   <li>Validar que pertenece a la sección del horario de la sesión.</li>
 *   <li>Verificar duplicado ({@code existsBySesionClaseIdAndEstudianteId}).</li>
 *   <li>Obtener hora del servidor ({@link Clock} inyectable).</li>
 *   <li>Calcular {@code PRESENTE} o {@code TARDANZA} según
 *       {@code toleranciaMinutos} del horario.</li>
 *   <li>Guardar la entidad {@link Asistencia}.</li>
 *   <li>Devolver {@link RegistrarAsistenciaResponse}.</li>
 * </ol>
 *
 * <p><strong>Nunca</strong> se confía en la hora enviada por el frontend
 * (07-PLAN 7.3 §6); se usa el reloj del servidor en cada llamada.
 *
 * <p>La operación es transaccional. La restricción UNIQUE de BD es la barrera
 * final ante condiciones de carrera (Prompt 7.5); si ocurre una violación
 * concurrente, se captura y se convierte en {@code ATTENDANCE_ALREADY_REGISTERED}.
 *
 * <h3>Códigos funcionales (07-PLAN 7.4):</h3>
 * <ul>
 *   <li>{@code SESSION_NOT_ACTIVE}        — sesión no está en estado ABIERTA.</li>
 *   <li>{@code STUDENT_NOT_FOUND}         — estudiante no hallado (delegado al resolver).</li>
 *   <li>{@code INVALID_QR}               — formato QR inválido (delegado al resolver).</li>
 *   <li>{@code STUDENT_NOT_IN_SECTION}    — estudiante no pertenece a la sección.</li>
 *   <li>{@code ATTENDANCE_ALREADY_REGISTERED} — asistencia ya registrada.</li>
 * </ul>
 */
@Service
public class AsistenciaService {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaService.class);

    static final String CODE_SESSION_NOT_FOUND        = "SESSION_NOT_FOUND";
    static final String CODE_SESSION_NOT_ACTIVE       = "SESSION_NOT_ACTIVE";
    static final String CODE_STUDENT_NOT_IN_SECTION   = "STUDENT_NOT_IN_SECTION";
    static final String CODE_ALREADY_REGISTERED       = "ATTENDANCE_ALREADY_REGISTERED";

    /** Nombre físico de la constraint UNIQUE en la tabla asistencias (V10). */
    private static final String UNIQUE_SESION_ESTUDIANTE = "uq_asistencia_sesion_estudiante";

    private final AsistenciaRepository asistenciaRepository;
    private final SesionClaseRepository sesionClaseRepository;
    private final EstudianteResolverService estudianteResolverService;
    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final AuditService auditService;
    private final Clock clock;

    public AsistenciaService(AsistenciaRepository asistenciaRepository,
                             SesionClaseRepository sesionClaseRepository,
                             EstudianteResolverService estudianteResolverService,
                             UsuarioRepository usuarioRepository,
                             DocenteRepository docenteRepository,
                             AuditService auditService,
                             Clock clock) {
        this.asistenciaRepository = asistenciaRepository;
        this.sesionClaseRepository = sesionClaseRepository;
        this.estudianteResolverService = estudianteResolverService;
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    // =========================================================================
    // Caso de uso principal
    // =========================================================================

    /**
     * Registra la asistencia de un estudiante a una sesión de clase.
     *
     * <p>La operación es transaccional; en caso de violación UNIQUE por
     * concurrencia el {@link DataIntegrityViolationException} se convierte en
     * {@link ConflictException} con código {@code ATTENDANCE_ALREADY_REGISTERED}.
     *
     * @param request datos del registro (sesionId, codigo, metodo).
     * @return respuesta con nombre, hora, estado y mensaje para Modo Aula.
     */
    @Transactional
    public RegistrarAsistenciaResponse registrar(RegistrarAsistenciaRequest request) {

        // Paso 1 — Validar sesión ABIERTA
        SesionClase sesion = obtenerSesionAbierta(request.sesionId());

        // Paso 2 — Resolver estudiante por QR o código
        Estudiante estudiante = estudianteResolverService.resolver(
                request.metodo(), request.codigo());

        // Paso 3 — Validar estudiante activo
        validarActivo(estudiante);

        // Paso 4 — Validar que pertenece a la sección del horario
        validarSeccion(estudiante, sesion);

        // Paso 5 — Verificar duplicado
        validarNoDuplicado(sesion.getId(), estudiante.getId());

        // Paso 6 — Hora del servidor
        OffsetDateTime ahora = OffsetDateTime.now(clock);

        // Paso 7 — Calcular estado: PRESENTE o TARDANZA
        EstadoAsistencia estado = calcularEstado(ahora, sesion);

        // Paso 8 — Guardar
        Asistencia asistencia = new Asistencia();
        asistencia.setSesionClase(sesion);
        asistencia.setEstudiante(estudiante);
        asistencia.setFechaHora(ahora);
        asistencia.setEstado(estado);
        asistencia.setMetodo(request.metodo());

        try {
            asistenciaRepository.save(asistencia);
            asistenciaRepository.flush();  // materializar para capturar violación UNIQUE en esta transacción
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null
                    && ex.getMessage().contains(UNIQUE_SESION_ESTUDIANTE)) {
                log.warn("Concurrencia: asistencia ya registrada para sesion={}, estudiante={}",
                        sesion.getId(), estudiante.getId());
                throw new ConflictException(
                        "Asistencia ya registrada para este estudiante en la sesión",
                        CODE_ALREADY_REGISTERED);
            }
            throw ex;
        }

        log.info("Asistencia registrada: sesion={}, estudiante={}, estado={}, metodo={}",
                sesion.getId(), estudiante.getId(), estado, request.metodo());

        // Paso 9 — Devolver respuesta
        String mensaje = (estado == EstadoAsistencia.PRESENTE)
                ? "Asistencia registrada: PRESENTE"
                : "Asistencia registrada: TARDANZA";

        return new RegistrarAsistenciaResponse(
                true,
                estudiante.getNombres(),   // solo nombres (07-PLAN 7.4: sin apellidos)
                ahora,
                estado,
                mensaje
        );
    }

    // =========================================================================
    // Corrección manual (Prompt 14.2)
    // =========================================================================

    /**
     * Permite a un docente o administrador cambiar manualmente el estado de una asistencia
     * justificándola u corrigiéndola (por ejemplo, de FALTA a JUSTIFICADO).
     */
    @Transactional
    public AsistenciaResponse correccionManual(Long id, AsistenciaCorreccionRequest request) {
        Asistencia asistencia = asistenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asistencia no encontrada: " + id, "ATTENDANCE_NOT_FOUND"));

        EstadoAsistencia estadoAnterior = asistencia.getEstado();
        EstadoAsistencia estadoNuevo = request.nuevoEstado();

        asistencia.setEstado(estadoNuevo);
        asistencia.setObservacion(request.motivo()); // Guardamos el motivo en la observación
        
        asistenciaRepository.save(asistencia);

        // Registro de auditoría
        String accion = estadoNuevo == EstadoAsistencia.JUSTIFICADO ? "JUSTIFICAR_ASISTENCIA" : "MODIFICAR_ASISTENCIA";
        
        // Creamos objetos anónimos o mapas para serializar los valores
        var valorAnterior = java.util.Map.of("estado", estadoAnterior);
        var valorNuevo = java.util.Map.of("estado", estadoNuevo, "motivo", request.motivo());
        
        auditService.registrar("asistencias", asistencia.getId(), accion, valorAnterior, valorNuevo);

        log.info("Asistencia {} corregida manualmente: de {} a {}. Motivo: {}", 
                 id, estadoAnterior, estadoNuevo, request.motivo());

        return toResponse(asistencia);
    }

    // =========================================================================
    // Consultas y Listados (Prompt 7.6)
    // =========================================================================

    /**
     * Lista asistencias aplicando múltiples filtros dinámicos (fecha, estado, sección, etc.).
     * DOCENTE autenticado solo verá las asistencias de las sesiones de sus horarios.
     * ADMIN verá todas, a menos que el filtro especifique explícitamente docente.
     */
    @Transactional(readOnly = true)
    public Page<AsistenciaResponse> listar(
            java.time.LocalDate fecha,
            EstadoAsistencia estado,
            Long seccionId,
            Long cursoId,
            Long estudianteId,
            Pageable pageable) {

        Specification<Asistencia> spec = Specification.where(
                com.aulaia.repository.specification.AsistenciaSpecification.porFecha(fecha)
                        .and(com.aulaia.repository.specification.AsistenciaSpecification.porEstado(estado))
                        .and(com.aulaia.repository.specification.AsistenciaSpecification.porSeccion(seccionId))
                        .and(com.aulaia.repository.specification.AsistenciaSpecification.porCurso(cursoId))
                        .and(com.aulaia.repository.specification.AsistenciaSpecification.porEstudiante(estudianteId))
        );

        if (esDocenteAutenticado()) {
            spec = spec.and(com.aulaia.repository.specification.AsistenciaSpecification.porDocente(docenteAutenticado().getId()));
        }

        return asistenciaRepository.findAll(spec, pageable).map(this::toResponse);
    }

    /**
     * Lista asistencias por sesión específica.
     * Respeta permisos: ADMIN ve todas; DOCENTE ve solo si es su sesión.
     */
    @Transactional(readOnly = true)
    public Page<AsistenciaResponse> listarPorSesion(Long sesionId, Pageable pageable) {
        Specification<Asistencia> spec = Specification.where(com.aulaia.repository.specification.AsistenciaSpecification.porSesion(sesionId));

        if (esDocenteAutenticado()) {
            spec = spec.and(com.aulaia.repository.specification.AsistenciaSpecification.porDocente(docenteAutenticado().getId()));
        }

        return asistenciaRepository.findAll(spec, pageable).map(this::toResponse);
    }

    /**
     * Lista asistencias de un estudiante específico (historial).
     * Estudiante autenticado podría ver el suyo (si implementado), DOCENTE solo en sus cursos.
     */
    @Transactional(readOnly = true)
    public Page<AsistenciaResponse> listarPorEstudiante(Long estudianteId, Pageable pageable) {
        Specification<Asistencia> spec = Specification.where(com.aulaia.repository.specification.AsistenciaSpecification.porEstudiante(estudianteId));

        if (esDocenteAutenticado()) {
            spec = spec.and(com.aulaia.repository.specification.AsistenciaSpecification.porDocente(docenteAutenticado().getId()));
        }

        return asistenciaRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private AsistenciaResponse toResponse(Asistencia asis) {
        return new AsistenciaResponse(
                asis.getId(),
                asis.getSesionClase().getId(),
                asis.getEstudiante().getId(),
                asis.getEstudiante().getNombres(),
                asis.getEstudiante().getApellidos(),
                asis.getFechaHora(),
                asis.getEstado(),
                asis.getMetodo(),
                asis.getObservacion()
        );
    }

    private boolean esDocenteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DOCENTE"::equals);
    }

    private Docente docenteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails principal)) {
            throw new ForbiddenOperationException("Autenticación requerida");
        }
        Usuario usuario = usuarioRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ForbiddenOperationException("Autenticación requerida"));
        return docenteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario no tiene un perfil docente asociado", "TEACHER_NOT_FOUND"));
    }

    // =========================================================================
    // Funciones de validación
    // =========================================================================

    /**
     * Paso 1: obtiene la sesión y valida que esté en estado ABIERTA.
     */
    private SesionClase obtenerSesionAbierta(Long sesionId) {
        SesionClase sesion = sesionClaseRepository.findById(sesionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sesión no encontrada: " + sesionId, CODE_SESSION_NOT_FOUND));

        if (sesion.getEstado() != SesionClaseEstado.ABIERTA) {
            log.warn("Sesión no activa: id={}, estado={}", sesionId, sesion.getEstado());
            throw new BusinessException(
                    "La sesión no está activa (estado=" + sesion.getEstado() + ")",
                    CODE_SESSION_NOT_ACTIVE);
        }
        return sesion;
    }

    /**
     * Paso 3: valida que el estudiante esté activo.
     */
    private void validarActivo(Estudiante estudiante) {
        if (!estudiante.isActivo()) {
            log.warn("Estudiante inactivo: id={}", estudiante.getId());
            throw new BusinessException(
                    "El estudiante no está activo en el sistema",
                    "STUDENT_INACTIVE");
        }
    }

    /**
     * Paso 4: valida que el estudiante pertenezca a la sección del horario.
     *
     * <p>La sesión tiene un horario, que tiene una sección. El estudiante
     * pertenece a una sección. Se compara por ID de sección.
     */
    private void validarSeccion(Estudiante estudiante, SesionClase sesion) {
        Horario horario = sesion.getHorario();
        Long seccionSesion = horario.getSeccion().getId();
        Long seccionEstudiante = estudiante.getSeccion().getId();

        if (!seccionSesion.equals(seccionEstudiante)) {
            log.warn("Estudiante id={} no pertenece a la sección id={} (pertenece a id={})",
                    estudiante.getId(), seccionSesion, seccionEstudiante);
            throw new BusinessException(
                    "El estudiante no pertenece a la sección de esta sesión",
                    CODE_STUDENT_NOT_IN_SECTION);
        }
    }

    /**
     * Paso 5: verifica que no exista ya una asistencia para el par
     * sesión+estudiante. La restricción UNIQUE en BD es la barrera final;
     * esta verificación optimista evita el fallo de BD en el caso nominal.
     */
    private void validarNoDuplicado(Long sesionId, Long estudianteId) {
        if (asistenciaRepository.existsBySesionClaseIdAndEstudianteId(sesionId, estudianteId)) {
            log.warn("Asistencia duplicada: sesion={}, estudiante={}", sesionId, estudianteId);
            throw new ConflictException(
                    "Asistencia ya registrada para este estudiante en la sesión",
                    CODE_ALREADY_REGISTERED);
        }
    }

    /**
     * Paso 7: determina si la asistencia es PRESENTE o TARDANZA.
     *
     * <p>Regla (06-FLUJOS, 04-BD §6.7 {@code tolerancia_minutos}):
     * <ul>
     *   <li>Si {@code ahora} está dentro de la ventana
     *       {@code [horaInicio, horaInicio + toleranciaMinutos)} → {@code PRESENTE}.</li>
     *   <li>Si {@code ahora} supera esa ventana pero la sesión sigue ABIERTA
     *       → {@code TARDANZA}.</li>
     * </ul>
     *
     * <p>La hora de inicio proviene del {@link Horario} de la sesión
     * (campo {@code horaInicio}). Se compara solo la hora local del día en la
     * zona del servidor (no se documenta zona específica de estudiante).
     */
    EstadoAsistencia calcularEstado(OffsetDateTime ahora, SesionClase sesion) {
        Horario horario = sesion.getHorario();
        LocalTime horaInicio = horario.getHoraInicio();
        int tolerancia = horario.getToleranciaMinutos();

        LocalTime horaActual = ahora.toLocalTime();
        LocalTime limite = horaInicio.plusMinutes(tolerancia);

        // Antes o durante la ventana de tolerancia → PRESENTE
        if (!horaActual.isAfter(limite)) {
            return EstadoAsistencia.PRESENTE;
        }
        // Fuera de la ventana pero la sesión está abierta → TARDANZA
        return EstadoAsistencia.TARDANZA;
    }
}
