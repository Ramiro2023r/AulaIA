package com.aulaia.service;

import com.aulaia.dto.curso.CursoRequest;
import com.aulaia.dto.curso.CursoResponse;
import com.aulaia.entity.Curso;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.CursoMapper;
import com.aulaia.repository.CursoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio del catálogo de cursos (Prompt 3.3).
 *
 * <p>La lógica vive aquí; el controller solo recibe, valida, delega y
 * responde. Autorización (solo ADMIN crea/modifica) la resuelve Spring
 * Security con {@code @PreAuthorize} en el controller, no aquí.
 *
 * <p>Sin reglas de unicidad: el modelo oficial (04-BD §6.6) no define
 * UNIQUE ni ninguna restricción de duplicados para {@code cursos}, por lo
 * que dos cursos pueden tener el mismo nombre. El servicio no agrega
 * validaciones de duplicados ni genera 409; la BD es la única fuente de
 * la verdad estructural y no impone esa restricción.
 */
@Service
public class CursoService {

    private static final Logger log = LoggerFactory.getLogger(CursoService.class);

    private static final String CODE_NOT_FOUND = "COURSE_NOT_FOUND";

    private final CursoRepository cursoRepository;
    private final CursoMapper cursoMapper;

    public CursoService(CursoRepository cursoRepository, CursoMapper cursoMapper) {
        this.cursoRepository = cursoRepository;
        this.cursoMapper = cursoMapper;
    }

    public List<CursoResponse> listar() {
        return cursoRepository.findAllByOrderByIdAsc().stream()
                .map(cursoMapper::toResponse)
                .toList();
    }

    public CursoResponse buscarPorId(Long id) {
        return cursoMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public CursoResponse crear(CursoRequest request) {
        Curso curso = cursoMapper.toEntity(request);
        curso.setNombre(normalizar(request.nombre()));
        curso.setDescripcion(normalizar(request.descripcion()));

        Curso guardado = cursoRepository.save(curso);
        log.info("Curso creado: id={}, nombre={}", guardado.getId(), guardado.getNombre());
        return cursoMapper.toResponse(guardado);
    }

    @Transactional
    public CursoResponse actualizar(Long id, CursoRequest request) {
        Curso curso = findOrThrow(id);
        curso.setNombre(normalizar(request.nombre()));
        curso.setDescripcion(normalizar(request.descripcion()));

        Curso guardado = cursoRepository.save(curso);
        log.info("Curso actualizado: id={}, nombre={}", guardado.getId(), guardado.getNombre());
        return cursoMapper.toResponse(guardado);
    }

    private Curso findOrThrow(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado: " + id, CODE_NOT_FOUND));
    }

    /** Normalización obvia: sin espacios al inicio/fin (no convierte datos inválidos en válidos). */
    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }
}