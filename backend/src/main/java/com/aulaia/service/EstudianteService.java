package com.aulaia.service;

import com.aulaia.dto.estudiante.EstudianteRequest;
import com.aulaia.dto.estudiante.EstudianteResponse;
import com.aulaia.dto.estudiante.RegenerarQrResponse;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.Seccion;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.EstudianteMapper;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.SeccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

/**
 * Reglas de negocio de estudiantes (Prompt 4.2, 07-PLAN).
 *
 * <p>La lógica vive aquí; el controller (Prompt 4.3) solo recibe, valida,
 * delega y responde. No se expone el {@code qrToken} en respuestas ni en
 * logs (privacidad: 04-BD §6.5 "no debe contener datos personales";
 * 07-PLAN 4.2 "nunca usar nombres o datos personales en el token").
 *
 * <p>Unicidad oficial (04-BD §6.5): {@code codigo} y {@code qr_token}
 * UNIQUE a nivel BD. El Service replica ambas reglas de forma preventiva y
 * la BD sigue siendo la barrera final; una violación concurrente se
 * convierte contextualmente (solo para la constraint correspondiente).
 */
@Service
public class EstudianteService {

    private static final Logger log = LoggerFactory.getLogger(EstudianteService.class);

    private static final String CODE_NOT_FOUND = "STUDENT_NOT_FOUND";
    private static final String CODE_ALREADY_EXISTS = "STUDENT_CODE_ALREADY_EXISTS";
    private static final String CODE_SECTION_NOT_FOUND = "SECTION_NOT_FOUND";

    /** Nombres reales de las constraints UNIQUE (PostgreSQL, UNIQUE inline de V6). */
    private static final String UNIQUE_CODIGO = "estudiantes_codigo_key";
    private static final String UNIQUE_QR_TOKEN = "estudiantes_qr_token_key";

    /** Longitud del token: 32 bytes aleatorios → 64 caracteres hex (≤ VARCHAR(120) de V6). */
    private static final int TOKEN_BYTES = 32;

    /** Prefijo del contenido QR (07-PLAN 4.5, 06-FLUJOS #7): AULAIA:STUDENT:<qrToken>. No se persiste. */
    public static final String PREFIJO_CONTENIDO_QR = "AULAIA:STUDENT:";

    /** Límite de reintentos ante colisión del qrToken (improbable; barrera final: UNIQUE BD). */
    private static final int TOKEN_MAX_REINTENTOS = 5;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EstudianteRepository estudianteRepository;
    private final SeccionRepository seccionRepository;
    private final EstudianteMapper estudianteMapper;
    private final AuditService auditService;

    public EstudianteService(EstudianteRepository estudianteRepository,
                             SeccionRepository seccionRepository,
                             EstudianteMapper estudianteMapper,
                             AuditService auditService) {
        this.estudianteRepository = estudianteRepository;
        this.seccionRepository = seccionRepository;
        this.estudianteMapper = estudianteMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<EstudianteResponse> listar() {
        return estudianteRepository.findAllByOrderByIdAsc().stream()
                .map(estudianteMapper::toResponse)
                .toList();
    }

    /**
     * Listado con los filtros documentados del Prompt 4.3 (07-PLAN):
     * codigo, nombre, seccion y activo, combinables entre sí (AND).
     *
     * <p>Semántica mínima elegida (los documentos no definen otra): codigo
     * = igualdad exacta (case-sensitive, coherente con la UNIQUE);
     * nombre = coincidencia parcial sobre el campo {@code nombres}
     * (interpretación literal del modelo y del buscador de la UI); seccion
     * = por id de sección; activo = true/false. Sin paginación (no
     * documentada en 07-PLAN 4.3). Orden estable por id (creación).
     */
    @Transactional(readOnly = true)
    public List<EstudianteResponse> listar(String codigo, String nombre, Long seccionId, Boolean activo) {
        Specification<Estudiante> spec = Specification.where(null);
        if (codigo != null && !codigo.isBlank()) {
            String valor = codigo.trim();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("codigo"), valor));
        }
        if (nombre != null && !nombre.isBlank()) {
            String valor = "%" + nombre.trim() + "%";
            spec = spec.and((root, query, cb) -> cb.like(root.get("nombres"), valor));
        }
        if (seccionId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("seccion").get("id"), seccionId));
        }
        if (activo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("activo"), activo));
        }
        return estudianteRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(estudianteMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstudianteResponse buscarPorId(Long id) {
        return estudianteMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public EstudianteResponse buscarPorCodigo(String codigo) {
        return estudianteMapper.toResponse(estudianteRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado: " + codigo, CODE_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public EstudianteResponse buscarPorQrToken(String qrToken) {
        return estudianteMapper.toResponse(estudianteRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado", CODE_NOT_FOUND)));
    }

    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        Seccion seccion = findSeccionOrThrow(request.seccionId());
        validarCodigoDisponible(request.codigo(), null);

        Estudiante estudiante = estudianteMapper.toEntity(request);
        estudiante.setCodigo(request.codigo().trim());
        estudiante.setNombres(request.nombres().trim());
        estudiante.setApellidos(request.apellidos().trim());
        estudiante.setSeccion(seccion);
        estudiante.setQrToken(generarQrTokenUnico());
        estudiante.setActivo(true);

        Estudiante guardado = guardar(estudiante);
        log.info("Estudiante creado: id={}, codigo={}", guardado.getId(), guardado.getCodigo());
        return estudianteMapper.toResponse(guardado);
    }

    @Transactional
    public EstudianteResponse actualizar(Long id, EstudianteRequest request) {
        Estudiante estudiante = findOrThrow(id);
        Seccion seccion = findSeccionOrThrow(request.seccionId());
        validarCodigoDisponible(request.codigo(), estudiante.getCodigo());

        estudiante.setCodigo(request.codigo().trim());
        estudiante.setNombres(request.nombres().trim());
        estudiante.setApellidos(request.apellidos().trim());
        estudiante.setSeccion(seccion);
        // qrToken se conserva: la regeneración corresponde al Prompt 4.4.

        Estudiante guardado = guardar(estudiante);
        log.info("Estudiante actualizado: id={}", guardado.getId());
        return estudianteMapper.toResponse(guardado);
    }

    @Transactional
    public EstudianteResponse desactivar(Long id) {
        Estudiante estudiante = findOrThrow(id);
        
        var valorAnterior = java.util.Map.of("activo", estudiante.isActivo());
        
        estudiante.setActivo(false);
        // Idempotente: si ya está inactivo, el resultado es el mismo y se conserva.
        Estudiante guardado = guardar(estudiante);
        
        var valorNuevo = java.util.Map.of("activo", guardado.isActivo());
        auditService.registrar("estudiantes", guardado.getId(), "DESACTIVAR_ESTUDIANTE", valorAnterior, valorNuevo);

        log.info("Estudiante desactivado: id={}", guardado.getId());
        return estudianteMapper.toResponse(guardado);
    }

    /**
     * Regeneración de QR (Prompt 4.4, 07-PLAN): genera un qrToken nuevo,
     * único y distinto del actual; el anterior queda inválido al dejar de
     * estar persistido. Solo ADMIN (controlado en el controller).
     *
     * <p>Sin bloqueo por estudiante inactivo: los documentos solo restringen
     * asistencia para inactivos (06-FLUJOS), no la regeneración de QR
     * (DECISIÓN NO DEFINIDA EN DOCUMENTOS reportada; sin regla inventada).
     *
     * <p>No modifica codigo, nombres, apellidos, seccion ni activo;
     * updatedAt se actualiza por {@code @PreUpdate} (estrategia vigente).
     * El token no se loguea ni se expone (privacidad: 04-BD §22).
     */
    @Transactional
    public RegenerarQrResponse regenerarQrToken(Long estudianteId) {
        Estudiante estudiante = findOrThrow(estudianteId);
        estudiante.setQrToken(generarQrTokenUnico(estudiante.getQrToken()));
        guardar(estudiante);

        // Registro de auditoría
        auditService.registrar("estudiantes", estudiante.getId(), "GENERAR_NUEVO_QR", null, null);

        log.info("QR de estudiante regenerado: id={}", estudiante.getId());
        return new RegenerarQrResponse(true);
    }

    /**
     * Contenido exacto del QR del estudiante (Prompt 4.5): {@code AULAIA:STUDENT:<qrToken>}.
     * El token no se regenera ni se modifica al consultar; el prefijo no se persiste.
     * Sin datos personales (04-BD §22: el QR solo contiene el token opaco).
     */
    @Transactional(readOnly = true)
    public String contenidoQr(Long estudianteId) {
        return PREFIJO_CONTENIDO_QR + findOrThrow(estudianteId).getQrToken();
    }

    private Estudiante findOrThrow(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado: " + id, CODE_NOT_FOUND));
    }

    private Seccion findSeccionOrThrow(Long seccionId) {
        return seccionRepository.findById(seccionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada: " + seccionId, CODE_SECTION_NOT_FOUND));
    }

    /**
     * Valida que el codigo esté disponible. {@code codigoActual} es el
     * valor persistido del estudiante en actualización (null en creación):
     * si el codigo no cambia, se permite sin consultas. Comparación exacta
     * (case-sensitive), igual que la UNIQUE física de PostgreSQL.
     */
    private void validarCodigoDisponible(String codigo, String codigoActual) {
        String codigoNormalizado = codigo.trim();
        if (codigoActual != null && codigoNormalizado.equals(codigoActual)) {
            return;
        }
        if (estudianteRepository.existsByCodigo(codigoNormalizado)) {
            throw new ConflictException("Ya existe un estudiante con el código '" + codigoNormalizado + "'", CODE_ALREADY_EXISTS);
        }
    }

    /**
     * Token opaco criptográficamente seguro (SecureRandom, 32 bytes → 64
     * caracteres hex). Sin datos personales, sin nombres, sin fechas, sin
     * secuencias. En caso de colisión (improbable) se reintenta hasta
     * {@value #TOKEN_MAX_REINTENTOS} veces; si se agota, falla de forma
     * controlada. La UNIQUE física de BD sigue siendo la barrera final.
     */
    private String generarQrTokenUnico() {
        return generarQrTokenUnico(null);
    }

    /**
     * Como {@link #generarQrTokenUnico()}, rechazando además el candidato
     * {@code tokenAExcluir} (el token actual del estudiante en una
     * regeneración, Prompt 4.4 §8): nunca se reutiliza el mismo token y se
     * evita que el propio estudiante cuente como colisión en la UNIQUE.
     */
    private String generarQrTokenUnico(String tokenAExcluir) {
        for (int intento = 1; intento <= TOKEN_MAX_REINTENTOS; intento++) {
            byte[] bytes = new byte[TOKEN_BYTES];
            SECURE_RANDOM.nextBytes(bytes);
            String token = HexFormat.of().formatHex(bytes);
            if (token.equals(tokenAExcluir)) {
                continue;
            }
            if (!estudianteRepository.existsByQrToken(token)) {
                return token;
            }
            log.warn("Colisión de qrToken en intento {}/{}", intento, TOKEN_MAX_REINTENTOS);
        }
        throw new IllegalStateException("No se pudo generar un qrToken único para el estudiante");
    }

    /**
     * Guarda con flush inmediato y convierte contextualmente la violación
     * de UNIQUE de BD: codigo → 409 STUDENT_CODE_ALREADY_EXISTS; colisión
     * extrema de qr_token → fallo controlado de generación. Cualquier otra
     * violación (p. ej. FK) se deja pasar sin enmascararla.
     */
    private Estudiante guardar(Estudiante estudiante) {
        try {
            return estudianteRepository.saveAndFlush(estudiante);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacion(ex, UNIQUE_CODIGO)) {
                throw new ConflictException("Ya existe un estudiante con ese código", CODE_ALREADY_EXISTS);
            }
            if (esViolacion(ex, UNIQUE_QR_TOKEN)) {
                throw new IllegalStateException("No se pudo generar un qrToken único para el estudiante", ex);
            }
            throw ex;
        }
    }

    private boolean esViolacion(DataIntegrityViolationException ex, String constraint) {
        Throwable causa = ex.getMostSpecificCause();
        return causa != null && causa.getMessage() != null && causa.getMessage().contains(constraint);
    }
}