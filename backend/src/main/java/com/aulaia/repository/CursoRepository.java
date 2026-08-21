package com.aulaia.repository;

import com.aulaia.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a datos de {@link Curso}.
 *
 * <p>Sin reglas de unicidad: el modelo oficial (04-BD §6.6) no define
 * UNIQUE sobre {@code nombre} ni ninguna otra restricción, por lo que el
 * repository no agrega consultas de existencia ni validaciones de
 * duplicados (la BD permite dos cursos con el mismo nombre).
 */
public interface CursoRepository extends JpaRepository<Curso, Long> {

    /** Listado con orden estable (creación) sin significado académico inventado. */
    List<Curso> findAllByOrderByIdAsc();
}