package com.aulaia.service;

import com.aulaia.dto.seccion.SeccionRequest;
import com.aulaia.dto.seccion.SeccionResponse;
import com.aulaia.entity.Grado;
import com.aulaia.entity.Seccion;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.SeccionMapper;
import com.aulaia.repository.GradoRepository;
import com.aulaia.repository.SeccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio del catálogo de secciones (Prompt 3.2).
 *
 * <p>La lógica vive aquí; el controller solo recibe, valida, delega y
 * responde. Autorización (solo ADMIN crea/modifica) la resuelve Spring
 * Security con {@code @PreAuthorize} en el controller, no aquí.
 *
 * <p>Restricción única oficial (04-BD §6.4, {@code uq_seccion_grado_periodo}):
 * dentro del mismo grado y mismo periodo académico no pueden existir dos
 * secciones con el mismo nombre. Se valida aquí con la misma semántica de la
 * UNIQUE física (case-sensitive, sin reglas adicionales) y la BD actúa como
 * última barrera: una {@code DataIntegrityViolationException} originada por
 * esa UNIQUE se convierte en 409 {@code SECTION_ALREADY_EXISTS} (la
 * conversión es contextual, solo para esta constraint).
 */
@Service
public class SeccionService {

    private static final Logger log = LoggerFactory.getLogger(SeccionService.class);

    private static final String CODE_NOT_FOUND = "SECTION_NOT_FOUND";
    private static final String CODE_ALREADY_EXISTS = "SECTION_ALREADY_EXISTS";
    private static final String CODE_GRADE_NOT_FOUND = "GRADE_NOT_FOUND";

    /** Nombre de la constraint UNIQUE en BD (docs/04-BASE_DE_DATOS §6.4). */
    private static final String UNIQUE_CONSTRAINT = "uq_seccion_grado_periodo";

    private final SeccionRepository seccionRepository;
    private final GradoRepository gradoRepository;
    private final SeccionMapper seccionMapper;

    public SeccionService(SeccionRepository seccionRepository,
                          GradoRepository gradoRepository,
                          SeccionMapper seccionMapper) {
        this.seccionRepository = seccionRepository;
        this.gradoRepository = gradoRepository;
        this.seccionMapper = seccionMapper;
    }

    @Transactional(readOnly = true)
    public List<SeccionResponse> listar() {
        return seccionRepository.findAllByOrderByIdAsc().stream()
                .map(seccionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeccionResponse buscarPorId(Long id) {
        return seccionMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public SeccionResponse crear(SeccionRequest request) {
        Grado grado = findGradoOrThrow(request.gradoId());
        validarNombreUnico(request.gradoId(), request.nombre(), request.periodoAcademico(), null);

        Seccion seccion = seccionMapper.toEntity(request);
        seccion.setGrado(grado);
        seccion.setNombre(normalizar(request.nombre()));
        seccion.setPeriodoAcademico(normalizar(request.periodoAcademico()));

        Seccion guardada = guardar(seccion);
        log.info("Sección creada: id={}, gradoId={}, nombre={}, periodo={}",
                guardada.getId(), guardada.getGrado().getId(), guardada.getNombre(), guardada.getPeriodoAcademico());
        return seccionMapper.toResponse(guardada);
    }

    @Transactional
    public SeccionResponse actualizar(Long id, SeccionRequest request) {
        Seccion seccion = findOrThrow(id);
        Grado grado = findGradoOrThrow(request.gradoId());
        validarNombreUnico(request.gradoId(), request.nombre(), request.periodoAcademico(), id);

        seccion.setGrado(grado);
        seccion.setNombre(normalizar(request.nombre()));
        seccion.setPeriodoAcademico(normalizar(request.periodoAcademico()));

        Seccion guardada = guardar(seccion);
        log.info("Sección actualizada: id={}, gradoId={}, nombre={}, periodo={}",
                guardada.getId(), guardada.getGrado().getId(), guardada.getNombre(), guardada.getPeriodoAcademico());
        return seccionMapper.toResponse(guardada);
    }

    private Seccion findOrThrow(Long id) {
        return seccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada: " + id, CODE_NOT_FOUND));
    }

    private Grado findGradoOrThrow(Long gradoId) {
        return gradoRepository.findById(gradoId)
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado: " + gradoId, CODE_GRADE_NOT_FOUND));
    }

    private void validarNombreUnico(Long gradoId, String nombre, String periodoAcademico, Long idExcluida) {
        String nombreNormalizado = normalizar(nombre);
        String periodoNormalizado = normalizar(periodoAcademico);
        boolean existe = idExcluida == null
                ? seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademico(
                        gradoId, nombreNormalizado, periodoNormalizado)
                : seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademicoAndIdNot(
                        gradoId, nombreNormalizado, periodoNormalizado, idExcluida);
        if (existe) {
            throw new ConflictException(
                    "Ya existe una sección '" + nombreNormalizado + "' en el grado " + gradoId
                            + " del periodo " + periodoNormalizado,
                    CODE_ALREADY_EXISTS);
        }
    }

    /**
     * Guarda con flush inmediato para capturar la UNIQUE de BD
     * ({@code uq_seccion_grado_periodo}) como 409 SECTION_ALREADY_EXISTS:
     * barrera final ante condiciones de carrera entre dos solicitudes
     * simultáneas. Cualquier otra violación de integridad se deja pasar
     * (la maneja el error global, sin exponer detalles).
     */
    private Seccion guardar(Seccion seccion) {
        try {
            return seccionRepository.saveAndFlush(seccion);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionDeUnicidad(ex)) {
                log.warn("UNIQUE {} violada al guardar sección", UNIQUE_CONSTRAINT);
                throw new ConflictException(
                        "Ya existe una sección con el mismo grado, nombre y periodo académico",
                        CODE_ALREADY_EXISTS);
            }
            throw ex;
        }
    }

    private boolean esViolacionDeUnicidad(DataIntegrityViolationException ex) {
        Throwable causa = ex.getMostSpecificCause();
        return causa != null && causa.getMessage() != null
                && causa.getMessage().contains(UNIQUE_CONSTRAINT);
    }

    /** Normalización obvia: sin espacios al inicio/fin (no convierte datos inválidos en válidos). */
    private String normalizar(String valor) {
        return valor.trim();
    }
}
