package com.aulaia.service;

import com.aulaia.dto.horario.HorarioRequest;
import com.aulaia.dto.horario.HorarioResponse;
import com.aulaia.entity.Curso;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Horario;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.BusinessException;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ForbiddenOperationException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.HorarioMapper;
import com.aulaia.repository.CursoRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.HorarioRepository;
import com.aulaia.repository.SeccionRepository;
import com.aulaia.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Reglas de negocio de horarios — Prompt 5.3 y 5.4 (07-PLAN).
 *
 * <p>Prompt 5.4: CRUD de horarios (03-ARQUITECTURA #24: GET, POST, PUT;
 * sin DELETE: los documentos no lo definen para horarios). Solo ADMIN crea
 * y modifica; DOCENTE consulta únicamente sus propios horarios (07-PLAN
 * 5.4: "DOCENTE puede consultar sus propios horarios").
 *
 * <p>Identidad del DOCENTE: se deriva SIEMPRE de la sesión/JWT (username
 * del principal → {@link Usuario} → {@link Docente} vía usuario_id, 1:1,
 * 04-BD §6.2). Nunca se confía en {@code docenteId} recibido del frontend
 * como prueba de identidad (Prompt 5.4 §11/§17). Un usuario DOCENTE sin
 * perfil docente es un error controlado (TEACHER_NOT_FOUND): no se
 * auto-crea el perfil.
 *
 * <p>Filtros documentados (07-PLAN 5.4): docente, seccion, curso y dia,
 * combinables con AND. Sin paginación ni filtros no documentados.
 * {@code activo} no es administrable por la API (ni el formulario 05-UI_UX
 * #29 ni el flujo 06-FLUJOS #11 lo capturan; sin operación de
 * desactivación documentada).
 */
@Service
public class HorarioService {

    private static final Logger log = LoggerFactory.getLogger(HorarioService.class);

    private static final String CODE_NOT_FOUND = "SCHEDULE_NOT_FOUND";
    private static final String CODE_COURSE_NOT_FOUND = "COURSE_NOT_FOUND";
    private static final String CODE_SECTION_NOT_FOUND = "SECTION_NOT_FOUND";
    private static final String CODE_TEACHER_NOT_FOUND = "TEACHER_NOT_FOUND";
    private static final String CODE_TEACHER_CONFLICT = "TEACHER_SCHEDULE_CONFLICT";
    private static final String CODE_SECTION_CONFLICT = "SECTION_SCHEDULE_CONFLICT";
    private static final String CODE_INVALID_DIA = "VALIDATION_ERROR";

    private final HorarioRepository horarioRepository;
    private final CursoRepository cursoRepository;
    private final SeccionRepository seccionRepository;
    private final DocenteRepository docenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final HorarioMapper horarioMapper;
    private final Validator validator;
    private final AuditService auditService;

    public HorarioService(HorarioRepository horarioRepository,
                          CursoRepository cursoRepository,
                          SeccionRepository seccionRepository,
                          DocenteRepository docenteRepository,
                          UsuarioRepository usuarioRepository,
                          HorarioMapper horarioMapper,
                          Validator validator,
                          AuditService auditService) {
        this.horarioRepository = horarioRepository;
        this.cursoRepository = cursoRepository;
        this.seccionRepository = seccionRepository;
        this.docenteRepository = docenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.horarioMapper = horarioMapper;
        this.validator = validator;
        this.auditService = auditService;
    }

    /**
     * Listado con filtros (07-PLAN 5.4). Un DOCENTE autenticado ve
     * únicamente sus horarios: el backend fuerza su docenteId derivado de
     * la sesión e ignora cualquier {@code docenteId} del request.
     */
    @Transactional(readOnly = true)
    public List<HorarioResponse> listarHorarios(Long docenteId, Long seccionId, Long cursoId, Short diaSemana) {
        validarDia(diaSemana);
        if (esDocenteAutenticado()) {
            docenteId = docenteAutenticado().getId();
        }
        return horarioRepository.buscarConFiltros(docenteId, seccionId, cursoId, diaSemana).stream()
                .map(horarioMapper::toResponse)
                .toList();
    }

    /**
     * Consulta por id. ADMIN puede ver cualquier horario; DOCENTE solo los
     * suyos (07-PLAN 5.4): horario de otro docente → 403 FORBIDDEN, sin
     * revelar más información de la necesaria.
     */
    @Transactional(readOnly = true)
    public HorarioResponse buscarHorario(Long id) {
        Horario horario = findOrThrow(id);
        if (esDocenteAutenticado()) {
            Docente docente = docenteAutenticado();
            if (!horario.getDocente().getId().equals(docente.getId())) {
                throw new ForbiddenOperationException(
                        "No puedes consultar horarios de otro docente");
            }
        }
        return horarioMapper.toResponse(horario);
    }

    /**
     * Crea un horario (06-FLUJOS #11): valida request, resuelve y valida
     * las relaciones (Curso/Seccion/Docente), valida las reglas propias y
     * los conflictos (Prompt 5.3), y persiste. {@code activo} queda en el
     * default oficial TRUE (04-BD §6.7).
     */
    @Transactional
    public HorarioResponse crear(HorarioRequest request) {
        Curso curso = cursoOrThrow(request.cursoId());
        Seccion seccion = seccionOrThrow(request.seccionId());
        Docente docente = docenteOrThrow(request.docenteId());

        Horario horario = horarioMapper.toEntity(request);
        horario.setCurso(curso);
        horario.setSeccion(seccion);
        horario.setDocente(docente);

        validarConflictos(horario);

        Horario guardado = horarioRepository.saveAndFlush(horario);
        log.info("Horario creado: id={}, docente={}, seccion={}, dia={}",
                guardado.getId(), docente.getId(), seccion.getId(), horario.getDiaSemana());
        return horarioMapper.toResponse(guardado);
    }

    /**
     * Actualiza un horario existente (sin upsert). Se reemplazan las
     * relaciones y campos administrables; {@code activo} se conserva (la
     * API no lo administra). La validación de conflictos excluye el propio
     * id: un horario nunca entra en conflicto consigo mismo.
     */
    @Transactional
    public HorarioResponse actualizar(Long id, HorarioRequest request) {
        Horario horario = findOrThrow(id);
        Curso curso = cursoOrThrow(request.cursoId());
        Seccion seccion = seccionOrThrow(request.seccionId());
        Docente docente = docenteOrThrow(request.docenteId());

        var valorAnterior = java.util.Map.of(
            "curso_id", horario.getCurso().getId(),
            "seccion_id", horario.getSeccion().getId(),
            "docente_id", horario.getDocente().getId(),
            "dia_semana", horario.getDiaSemana(),
            "hora_inicio", horario.getHoraInicio(),
            "hora_fin", horario.getHoraFin(),
            "tolerancia_minutos", horario.getToleranciaMinutos(),
            "minutos_antes_apertura", horario.getMinutosAntesApertura()
        );

        horario.setCurso(curso);
        horario.setSeccion(seccion);
        horario.setDocente(docente);
        horario.setDiaSemana(request.diaSemana());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        horario.setToleranciaMinutos(request.toleranciaMinutos());
        horario.setMinutosAntesApertura(request.minutosAntesApertura());

        validarConflictos(horario);

        Horario guardado = horarioRepository.saveAndFlush(horario);
        
        var valorNuevo = java.util.Map.of(
            "curso_id", guardado.getCurso().getId(),
            "seccion_id", guardado.getSeccion().getId(),
            "docente_id", guardado.getDocente().getId(),
            "dia_semana", guardado.getDiaSemana(),
            "hora_inicio", guardado.getHoraInicio(),
            "hora_fin", guardado.getHoraFin(),
            "tolerancia_minutos", guardado.getToleranciaMinutos(),
            "minutos_antes_apertura", guardado.getMinutosAntesApertura()
        );
        
        auditService.registrar("horarios", guardado.getId(), "MODIFICAR_HORARIO", valorAnterior, valorNuevo);

        log.info("Horario actualizado: id={}", guardado.getId());
        return horarioMapper.toResponse(guardado);
    }

    /**
     * Prompt 5.3: valida que {@code horario} pueda guardarse sin
     * conflictos. Primero las reglas propias del horario (día 1-7,
     * horaFin &gt; horaInicio, tolerancia &gt;= 0, apertura &gt;= 0),
     * luego conflicto de docente y de sección (07-PLAN 5.3, 06-FLUJOS #11).
     * En actualización, {@code horario.getId()} no nulo excluye al propio
     * horario de las consultas.
     *
     * @throws ConstraintViolationException si viola reglas propias (400)
     * @throws ConflictException             si hay conflicto de docente o
     *                                       sección (409)
     */
    @Transactional(readOnly = true)
    public void validarConflictos(Horario horario) {
        Set<ConstraintViolation<Horario>> violaciones = validator.validate(horario);
        if (!violaciones.isEmpty()) {
            throw new ConstraintViolationException(violaciones);
        }

        Long id = horario.getId();
        if (horarioRepository.existeConflictoDocente(
                horario.getDocente().getId(), horario.getDiaSemana(),
                horario.getHoraInicio(), horario.getHoraFin(), id)) {
            throw new ConflictException(
                    "El docente ya tiene un horario solapado ese día", CODE_TEACHER_CONFLICT);
        }
        if (horarioRepository.existeConflictoSeccion(
                horario.getSeccion().getId(), horario.getDiaSemana(),
                horario.getHoraInicio(), horario.getHoraFin(), id)) {
            throw new ConflictException(
                    "La sección ya tiene un horario solapado ese día", CODE_SECTION_CONFLICT);
        }
    }

    private Horario findOrThrow(Long id) {
        return horarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado: " + id, CODE_NOT_FOUND));
    }

    private Curso cursoOrThrow(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado: " + id, CODE_COURSE_NOT_FOUND));
    }

    private Seccion seccionOrThrow(Long id) {
        return seccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada: " + id, CODE_SECTION_NOT_FOUND));
    }

    private Docente docenteOrThrow(Long id) {
        return docenteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado: " + id, CODE_TEACHER_NOT_FOUND));
    }

    private void validarDia(Short diaSemana) {
        if (diaSemana != null && (diaSemana < 1 || diaSemana > 7)) {
            throw new BusinessException("diaSemana debe estar entre 1 y 7", CODE_INVALID_DIA);
        }
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
     * Deriva el {@link Docente} del principal autenticado (Prompt 5.4
     * §17): username de la sesión → Usuario → perfil Docente (1:1,
     * 04-BD §6.2). Un DOCENTE sin perfil es un error controlado.
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