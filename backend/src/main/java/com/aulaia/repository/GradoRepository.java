package com.aulaia.repository;

import com.aulaia.entity.Grado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a datos de {@link Grado}.
 *
 * <p>Consultas derivadas simples. El modelo oficial (04-BD §6.3) no define
 * unicidad sobre {@code nombre}: dos grados pueden compartir nombre.
 */
public interface GradoRepository extends JpaRepository<Grado, Long> {

    /** Listado con orden estable (creación) sin significado académico inventado. */
    List<Grado> findAllByOrderByIdAsc();
}