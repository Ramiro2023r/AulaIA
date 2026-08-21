package com.aulaia.repository;

import com.aulaia.entity.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a datos de {@link Seccion}.
 *
 * <p>La unicidad compuesta {@code (grado_id, nombre, periodo_academico)}
 * SÍ es una restricción oficial (04-BD §6.4, constraint
 * {@code uq_seccion_grado_periodo} a nivel BD). Estas consultas replican
 * exactamente la semántica de la UNIQUE física: case-sensitive, sin
 * reglas adicionales no documentadas (la collation actual de PostgreSQL
 * distingue mayúsculas/minúsculas).
 */
public interface SeccionRepository extends JpaRepository<Seccion, Long> {

    /** Listado con orden estable (creación) sin significado académico inventado. */
    List<Seccion> findAllByOrderByIdAsc();

    boolean existsByGradoIdAndNombreAndPeriodoAcademico(
            Long gradoId, String nombre, String periodoAcademico);

    boolean existsByGradoIdAndNombreAndPeriodoAcademicoAndIdNot(
            Long gradoId, String nombre, String periodoAcademico, Long id);
}
