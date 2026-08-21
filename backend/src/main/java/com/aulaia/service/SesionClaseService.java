package com.aulaia.service;

import com.aulaia.dto.sesion.SesionClaseResponse;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Horario;
import com.aulaia.entity.SesionClase;
import com.aulaia.entity.SesionClaseEstado;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ForbiddenOperationException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.SesionClaseMapper;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.HorarioRepository;
import com.aulaia.repository.SesionClaseRepository;
import com.aulaia.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aulaia.entity.Asistencia;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.MetodoRegistro;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reglas de negocio de sesiones de clase — Prompts 6.2 y 6.3 (07-PLAN).
 *
 * <p><b>6.2</b> — {@link #obtenerOCrearSesion}: obtiene o crea la sesión
 * real correspondiente a un horario y una fecha (06-FLUJOS #12: la sesión
 * es la ocurrencia real de la plantilla; flujo de apertura #13: "¿Sesión ya
 * existe? SÍ → Usar sesión existente; NO → Crear sesión"). Sin duplicados
 * (07-PLAN 6.2).
 *
 * <p><b>6.3</b> — {@link #abrirSesion}: abre una sesión existente
 * (POST /api/v1/sesiones/{id}/abrir, 07-PLAN 6.3). La sesión nace con el
 * estado inicial documentado {@code PROGRAMADA} (04-BD §7.1 DEFAULT) y con
 * {@code horaApertura}/{@code horaCierre} en null (nullability
 * documentada). Se valida: sesión existe (404 {@code SESSION_NOT_FOUND}),
 * permiso (ADMIN o DOCENTE propietario del horario, Prompt 6.3 §9) y estado
 * permitido. Al abrir: {@code estado = ABIERTA} y
 * {@code horaApertura = hora actual del servidor} ({@link Clock}
 * inyectable, Prompt 6.3 §5-§6); {@code horaCierre} permanece null.
 *
 * <p><b>Estado permitido</b> (semántica mínima coherente, decisión
 * reportada): únicamente {@code PROGRAMADA} transita a {@code ABIERTA}.
 * Una sesión ya {@code ABIERTA} es idempotente (200, se devuelve tal cual,
 * jamás se reemplaza {@code horaApertura}); {@code CERRADA} y
 * {@code CANCELADA} se rechazan (409 {@code SESSION_INVALID_STATE}): no hay
 * regla documental de reapertura (06-FLUJOS #55: "Solo si se habilita", no
 * habilitada) ni de recuperación de cancelación (06-FLUJOS #54).
 *
 * <p><b>No validados por falta de regla documental</b> (decisiones
 * reportadas): ventana de minutos antes de apertura (06-FLUJOS #53: "La
 * política exacta debe definirse con la institución"), fecha pasada/futura
 * (06-FLUJOS #13 menciona "validar fecha" sin regla exacta) y horario
 * inactivo. El día de la semana frente a {@code fecha} tampoco se valida:
 * no hay regla documental al respecto.
 *
 * <p>Identidad del DOCENTE: derivada SIEMPRE de la sesión/JWT (username
 * del principal → {@link Usuario} → {@link Docente} vía usuario_id, 1:1,
 * 04-BD §6.2), nunca de parámetros del request (Prompt 6.3 §10). Un
 * usuario DOCENTE sin perfil docente es un error controlado
 * (TEACHER_NOT_FOUND, patrón de Horarios 5.4): no se auto-crea el perfil.
 * Un usuario inactivo no autentica (validación en el filtro JWT), por lo
 * que no se duplica esa regla aquí.
 *
 * <p>Idempotente en 6.2: llamadas repetidas con el mismo horario+fecha
 * devuelven siempre la misma sesión persistida. La UNIQUE física
 * {@code uq_sesion_horario_fecha} (04-BD §7.1) es la barrera final contra
 * duplicados bajo concurrencia; la carrera se recupera en
 * {@link SesionClaseCreatorTx} re-consultando la fila persistida.
 */
@Service
public class SesionClaseService {

    private static final Logger log = LoggerFactory.getLogger(SesionClaseService.class);

    private static final String CODE_SCHEDULE_NOT_FOUND = "SCHEDULE_NOT_FOUND";
    private static final String CODE_SESSION_NOT_FOUND = "SESSION_NOT_FOUND";
    private static final String CODE_SESSION_INVALID_STATE = "SESSION_INVALID_STATE";
    private static final String CODE_TEACHER_NOT_FOUND = "TEACHER_NOT_FOUND";

    private final HorarioRepository horarioRepository;
    private final SesionClaseRepository sesionClaseRepository;
    private final SesionClaseCreatorTx sesionClaseCreatorTx;
    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EstudianteRepository estudianteRepository;
    private final SesionClaseMapper sesionClaseMapper;
    private final AuditService auditService;
    private final Clock clock;

    public SesionClaseService(HorarioRepository horarioRepository,
                              SesionClaseRepository sesionClaseRepository,
                              SesionClaseCreatorTx sesionClaseCreatorTx,
                              UsuarioRepository usuarioRepository,
                              DocenteRepository docenteRepository,
                              AsistenciaRepository asistenciaRepository,
                              EstudianteRepository estudianteRepository,
                              SesionClaseMapper sesionClaseMapper,
                              AuditService auditService,
                              Clock clock) {
        this.horarioRepository = horarioRepository;
        this.sesionClaseRepository = sesionClaseRepository;
        this.sesionClaseCreatorTx = sesionClaseCreatorTx;
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.estudianteRepository = estudianteRepository;
        this.sesionClaseMapper = sesionClaseMapper;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Obtiene la sesión existente para {@code horarioId}+{@code fecha} o
     * la crea si no existe (07-PLAN 6.2, 06-FLUJOS #13). Nunca crea
     * duplicados: si otra petición ganó la carrera de la UNIQUE, devuelve
     * la sesión persistida. Si {@code horarioId} no existe → 404
     * {@code SCHEDULE_NOT_FOUND} (código reutilizado de Horarios).
     */
    @Transactional
    public SesionClase obtenerOCrearSesion(Long horarioId, LocalDate fecha) {
        Horario horario = horarioOrThrow(horarioId);
        Optional<SesionClase> existente = sesionClaseRepository.findByHorarioIdAndFecha(horarioId, fecha);
        if (existente.isPresent()) {
            return existente.get();
        }
        return crearOConsultar(horario, fecha);
    }

    /**
     * Abre una sesión existente (07-PLAN 6.3, 06-FLUJOS #13): valida
     * existencia (404 {@code SESSION_NOT_FOUND}), permiso (ADMIN, o DOCENTE
     * propietario del horario; si no → 403 {@code FORBIDDEN}) y estado
     * permitido (solo {@code PROGRAMADA} → {@code ABIERTA};
     * {@code ABIERTA} es idempotente; {@code CERRADA}/{@code CANCELADA} →
     * 409 {@code SESSION_INVALID_STATE}). Establece
     * {@code horaApertura = hora actual del servidor} ({@link Clock}
     * inyectable); no modifica fecha, horario ni {@code horaCierre}.
     *
     * <p>Este endpoint trabaja con la sesión identificada por {@code id}:
     * nunca convierte la llamada en un obtener-o-crear (la creación por
     * horario+fecha pertenece a 6.2 y el contrato de abrir no lo altera).
     */
    @Transactional
    public SesionClaseResponse abrirSesion(Long id) {
        SesionClase sesion = sesionClaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sesión no encontrada: " + id, CODE_SESSION_NOT_FOUND));

        validarPermisoDeApertura(sesion);

        if (sesion.getEstado() == SesionClaseEstado.ABIERTA) {
            log.info("Sesión ya abierta (idempotente): id={}, horaApertura={}",
                    sesion.getId(), sesion.getHoraApertura());
            return sesionClaseMapper.toResponse(sesion);
        }
        if (sesion.getEstado() != SesionClaseEstado.PROGRAMADA) {
            throw new ConflictException(
                    "La sesión no puede abrirse desde el estado " + sesion.getEstado(),
                    CODE_SESSION_INVALID_STATE);
        }

        sesion.setEstado(SesionClaseEstado.ABIERTA);
        sesion.setHoraApertura(OffsetDateTime.now(clock));
        SesionClase guardada = sesionClaseRepository.saveAndFlush(sesion);
        log.info("Sesión abierta: id={}, horaApertura={}",
                guardada.getId(), guardada.getHoraApertura());
        return sesionClaseMapper.toResponse(guardada);
    }

    /**
     * Cerrar sesión y generar ausencias para los estudiantes inasistentes (Prompt 7.7).
     * 1. Validar sesión ABIERTA.
     * 2. Obtener estudiantes activos de la sección.
     * 3. Identificar quiénes no tienen asistencia.
     * 4. Crear AUSENTE con método SISTEMA.
     * 5. Cambiar estado a CERRADA y registrar horaCierre.
     */
    @Transactional
    public SesionClaseResponse cerrarSesion(Long id) {
        SesionClase sesion = sesionClaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sesión no encontrada: " + id, CODE_SESSION_NOT_FOUND));

        validarPermisoDeApertura(sesion);

        if (sesion.getEstado() == SesionClaseEstado.CERRADA) {
            log.info("Sesión ya cerrada (idempotente): id={}, horaCierre={}",
                    sesion.getId(), sesion.getHoraCierre());
            return sesionClaseMapper.toResponse(sesion);
        }
        if (sesion.getEstado() == SesionClaseEstado.CANCELADA) {
            throw new ConflictException(
                    "La sesión no puede cerrarse porque está CANCELADA",
                    CODE_SESSION_INVALID_STATE);
        }
        if (sesion.getEstado() != SesionClaseEstado.ABIERTA) {
            throw new ConflictException(
                    "La sesión no puede cerrarse desde el estado " + sesion.getEstado(),
                    CODE_SESSION_INVALID_STATE);
        }

        Long seccionId = sesion.getHorario().getSeccion().getId();
        List<Estudiante> activos = estudianteRepository.findBySeccionIdAndActivoTrue(seccionId);

        Set<Long> conAsistencia = asistenciaRepository.findBySesionClaseId(id).stream()
                .map(a -> a.getEstudiante().getId())
                .collect(Collectors.toSet());

        List<Asistencia> ausentes = activos.stream()
                .filter(e -> !conAsistencia.contains(e.getId()))
                .map(e -> {
                    Asistencia asis = new Asistencia();
                    asis.setSesionClase(sesion);
                    asis.setEstudiante(e);
                    asis.setEstado(EstadoAsistencia.AUSENTE);
                    asis.setMetodo(MetodoRegistro.SISTEMA);
                    asis.setFechaHora(OffsetDateTime.now(clock));
                    return asis;
                })
                .toList();

        if (!ausentes.isEmpty()) {
            asistenciaRepository.saveAll(ausentes);
            log.info("Sesión {}: generadas {} inasistencias automáticas", id, ausentes.size());
        }

        sesion.setEstado(SesionClaseEstado.CERRADA);
        sesion.setHoraCierre(OffsetDateTime.now(clock));
        SesionClase guardada = sesionClaseRepository.saveAndFlush(sesion);

        auditService.registrar("sesiones_clase", guardada.getId(), "CERRAR_SESION", null, null);

        return sesionClaseMapper.toResponse(guardada);
    }

    /**
     * Cancelar sesión (Prompt 14.1).
     */
    @Transactional
    public SesionClaseResponse cancelarSesion(Long id) {
        SesionClase sesion = sesionClaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sesión no encontrada: " + id, CODE_SESSION_NOT_FOUND));

        validarPermisoDeApertura(sesion);

        if (sesion.getEstado() == SesionClaseEstado.CANCELADA) {
            return sesionClaseMapper.toResponse(sesion);
        }

        if (sesion.getEstado() == SesionClaseEstado.CERRADA) {
            throw new ConflictException(
                    "La sesión no puede cancelarse porque ya está CERRADA",
                    CODE_SESSION_INVALID_STATE);
        }

        sesion.setEstado(SesionClaseEstado.CANCELADA);
        SesionClase guardada = sesionClaseRepository.saveAndFlush(sesion);

        auditService.registrar("sesiones_clase", guardada.getId(), "CANCELAR_SESION", null, null);

        return sesionClaseMapper.toResponse(guardada);
    }

    /**
     * Listado de sesiones con los filtros documentados del Prompt 6.4
     * (07-PLAN: fecha, docente, seccion, curso, estado), combinables con
     * AND. Sin paginación ni orden configurable (no documentados). Un
     * DOCENTE autenticado ve únicamente las sesiones de sus horarios: el
     * backend fuerza su docenteId derivado de la sesión e ignora cualquier
     * {@code docenteId} del request.
     */
    @Transactional(readOnly = true)
    public List<SesionClaseResponse> listarSesiones(LocalDate fecha, Long docenteId,
                                                    Long seccionId, Long cursoId,
                                                    SesionClaseEstado estado) {
        Long docenteEfectivo = esDocenteAutenticado() ? docenteAutenticado().getId() : docenteId;
        return sesionClaseRepository.buscarConFiltros(fecha, docenteEfectivo, seccionId, cursoId, estado)
                .stream()
                .map(sesionClaseMapper::toResponse)
                .toList();
    }

    /**
     * Sesiones "activas" (GET /api/v1/sesiones/activas, 07-PLAN 6.4).
     * Semántica mínima coherente aplicada: {@code estado = ABIERTA}, la
     * única acepción documentada de "sesión activa" en el sistema
     * (02-TRD §11: "AsistenciaService busca la sesión activa y valida
     * estado ABIERTA"; 03-ARQ #26 "Buscar sesión activa"; 04-BD §17);
     * decisión reportada: DECISIÓN NO DEFINIDA EN DOCUMENTOS (el listado
     * no enumera estados, se aplica la semántica coherente con el resto de
     * documentos). Sin filtros de query params (no documentados para
     * /activas); solo el ownership del DOCENTE autenticado.
     */
    @Transactional(readOnly = true)
    public List<SesionClaseResponse> listarSesionesActivas() {
        Long docenteEfectivo = esDocenteAutenticado() ? docenteAutenticado().getId() : null;
        return sesionClaseRepository.buscarConFiltros(null, docenteEfectivo, null, null,
                        SesionClaseEstado.ABIERTA)
                .stream()
                .map(sesionClaseMapper::toResponse)
                .toList();
    }

    /**
     * Consulta por id (GET /api/v1/sesiones/{id}, 07-PLAN 6.4). ADMIN
     * puede consultar cualquier sesión; DOCENTE solo las de su propio
     * horario (403 {@code FORBIDDEN} si es de otro docente, sin revelar
     * información de la necesaria). Sesión inexistente → 404
     * {@code SESSION_NOT_FOUND} (código de 6.3).
     */
    @Transactional(readOnly = true)
    public SesionClaseResponse buscarSesionPorId(Long id) {
        SesionClase sesion = sesionClaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sesión no encontrada: " + id, CODE_SESSION_NOT_FOUND));
        if (esDocenteAutenticado()) {
            Docente docente = docenteAutenticado();
            if (!sesion.getHorario().getDocente().getId().equals(docente.getId())) {
                throw new ForbiddenOperationException("No puedes consultar sesiones de otro docente");
            }
        }
        return sesionClaseMapper.toResponse(sesion);
    }

    /**
     * Prompt 6.3 §9-§11: ADMIN puede abrir cualquier sesión; DOCENTE solo
     * las de su propio horario (SesionClase → Horario → Docente → Usuario,
     * con la identidad derivada del JWT). DOCENTE ajeno → 403 FORBIDDEN,
     * sin revelar más información de la necesaria.
     */
    private void validarPermisoDeApertura(SesionClase sesion) {
        if (!esDocenteAutenticado()) {
            return;
        }
        Docente docente = docenteAutenticado();
        if (!sesion.getHorario().getDocente().getId().equals(docente.getId())) {
            throw new ForbiddenOperationException("No puedes abrir una sesión de otro docente");
        }
    }

    /**
     * Intenta crear; si la UNIQUE indica que otra petición ya persistió la
     * sesión (carrera perdida), re-consulta y devuelve la fila existente.
     */
    private SesionClase crearOConsultar(Horario horario, LocalDate fecha) {
        SesionClase creada = sesionClaseCreatorTx.crearSesion(horario, fecha);
        if (creada != null) {
            log.info("Sesión creada: horario={}, fecha={}, id={}",
                    horario.getId(), fecha, creada.getId());
            return creada;
        }
        return sesionClaseRepository.findByHorarioIdAndFecha(horario.getId(), fecha)
                .orElseThrow(() -> new IllegalStateException(
                        "Sesión no recuperable tras creación concurrente"));
    }

    private Horario horarioOrThrow(Long horarioId) {
        return horarioRepository.findById(horarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Horario no encontrado: " + horarioId, CODE_SCHEDULE_NOT_FOUND));
    }

    /**
     * ¿El principal autenticado tiene rol DOCENTE? Se lee de las
     * authorities (derivadas del JWT); nunca de parámetros del request.
     */
    private boolean esDocenteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DOCENTE"::equals);
    }

    /**
     * Deriva el {@link Docente} del principal autenticado (Prompt 6.3 §10,
     * patrón Horarios 5.4): username de la sesión → Usuario → perfil
     * Docente (1:1, 04-BD §6.2). Un DOCENTE sin perfil es un error
     * controlado (TEACHER_NOT_FOUND), sin auto-creación de perfil.
     */
    private Docente docenteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails principal)) {
            throw new ForbiddenOperationException("Autenticación requerida");
        }
        Usuario usuario = usuarioRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ForbiddenOperationException("Autenticación requerida"));
        return docenteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario no tiene un perfil docente asociado", CODE_TEACHER_NOT_FOUND));
    }
}