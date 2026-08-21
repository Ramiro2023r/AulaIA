package com.aulaia.service;

import com.aulaia.dto.grado.GradoRequest;
import com.aulaia.dto.grado.GradoResponse;
import com.aulaia.entity.Grado;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.GradoMapper;
import com.aulaia.repository.GradoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Reglas de negocio del catálogo de grados (Prompt 3.1).
 *
 * <p>La lógica vive aquí; el controller solo recibe, valida, delega y
 * responde. Autorización (solo ADMIN crea/modifica) la resuelve Spring
 * Security con {@code @PreAuthorize} en el controller, no aquí.
 *
 * <p>Nota de diseño: el modelo oficial (04-BD §6.3) no define unicidad
 * sobre {@code nombre}; el módulo NO rechaza nombres repetidos (dos grados
 * pueden compartir nombre).
 */
@Service
public class GradoService {

    private static final Logger log = LoggerFactory.getLogger(GradoService.class);

    /** Default del modelo oficial para {@code nivel} (04-BD §6.3). */
    private static final String NIVEL_DEFAULT = "PRIMARIA";

    private static final String CODE_NOT_FOUND = "GRADE_NOT_FOUND";

    private final GradoRepository gradoRepository;
    private final GradoMapper gradoMapper;

    public GradoService(GradoRepository gradoRepository, GradoMapper gradoMapper) {
        this.gradoRepository = gradoRepository;
        this.gradoMapper = gradoMapper;
    }

    public List<GradoResponse> listar() {
        return gradoRepository.findAllByOrderByIdAsc().stream()
                .map(gradoMapper::toResponse)
                .toList();
    }

    public GradoResponse buscarPorId(Long id) {
        return gradoMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public GradoResponse crear(GradoRequest request) {
        Grado grado = gradoMapper.toEntity(request);
        grado.setNombre(normalizarNombre(request.nombre()));
        grado.setNivel(normalizarNivel(request.nivel()));

        Grado guardado = gradoRepository.save(grado);
        log.info("Grado creado: id={}, nombre={}, nivel={}", guardado.getId(), guardado.getNombre(), guardado.getNivel());
        return gradoMapper.toResponse(guardado);
    }

    @Transactional
    public GradoResponse actualizar(Long id, GradoRequest request) {
        Grado grado = findOrThrow(id);

        grado.setNombre(normalizarNombre(request.nombre()));
        grado.setNivel(normalizarNivel(request.nivel()));
        grado.setOrden(request.orden());

        Grado guardado = gradoRepository.save(grado);
        log.info("Grado actualizado: id={}", guardado.getId());
        return gradoMapper.toResponse(guardado);
    }

    private Grado findOrThrow(Long id) {
        return gradoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado: " + id, CODE_NOT_FOUND));
    }

    /** Normalización obvia: sin espacios al inicio/fin (no convierte datos inválidos en válidos). */
    private String normalizarNombre(String nombre) {
        return nombre.trim();
    }

    /** Blank → default del modelo oficial; si viene, se recorta. */
    private String normalizarNivel(String nivel) {
        return StringUtils.hasText(nivel) ? nivel.trim() : NIVEL_DEFAULT;
    }
}