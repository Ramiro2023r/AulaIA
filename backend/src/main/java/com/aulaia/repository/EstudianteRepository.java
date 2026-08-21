package com.aulaia.repository;

import com.aulaia.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de {@link Estudiante}.
 *
 * <p>Consultas derivadas simples. El modelo oficial (04-BD §6.5) define
 * UNIQUE sobre {@code codigo} y {@code qr_token}: estas consultas
 * existen por la necesidad documentada de {@code buscarPorCodigo} y
 * {@code buscarPorQrToken} del Prompt 4.2 (07-PLAN) y para probar las
 * restricciones reales en este prompt.
 *
 * <p>Los filtros documentados del Prompt 4.3 (codigo, nombre, seccion,
 * activo, combinables) se resuelven con {@code Specification} de Spring
 * Data JPA (sin librerías externas); el orden estable por id lo aplica el
 * servicio.
 */
public interface EstudianteRepository extends JpaRepository<Estudiante, Long>, JpaSpecificationExecutor<Estudiante> {

    Optional<Estudiante> findByCodigo(String codigo);

    Optional<Estudiante> findByQrToken(String qrToken);

    boolean existsByCodigo(String codigo);

    boolean existsByQrToken(String qrToken);

    /** Listado con orden estable (creación) sin significado académico inventado. */
    List<Estudiante> findAllByOrderByIdAsc();
    
    /** 
     * Obtiene los estudiantes activos de una sección determinada (usado para cierre de sesión).
     */
    List<Estudiante> findBySeccionIdAndActivoTrue(Long seccionId);
    
    /**
     * Cuenta los estudiantes activos de una sección.
     */
    long countBySeccionIdAndActivoTrue(Long seccionId);
}