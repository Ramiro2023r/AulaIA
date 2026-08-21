package com.aulaia.repository;

import com.aulaia.entity.SesionClase;
import com.aulaia.entity.SesionClaseEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de {@link SesionClase} (tabla {@code sesiones_clase},
 * 04-BD §7.1).
 *
 * <p>Prompt 6.1/6.2: verificación de la UNIQUE física
 * {@code uq_sesion_horario_fecha} (horario_id, fecha) y obtener-o-crear.
 * Prompt 6.4: listado con los filtros documentados (07-PLAN 6.4: fecha,
 * docente, seccion, curso, estado), combinables entre sí (AND), con orden
 * estable por id (creación). Para DOCENTE el Service fuerza siempre su
 * propio docenteId: el parámetro nunca es autorización. El estado de
 * "sesiones activas" se resuelve en el Service con {@code ABIERTA}
 * (semántica documentada de "sesión activa", 02-TRD §11 y 03-ARQ #26).
 */
public interface SesionClaseRepository extends JpaRepository<SesionClase, Long> {

    Optional<SesionClase> findByHorarioIdAndFecha(Long horarioId, LocalDate fecha);

    boolean existsByHorarioIdAndFecha(Long horarioId, LocalDate fecha);

    /**
     * Listado con los filtros documentados del Prompt 6.4 (07-PLAN):
     * fecha, docente, seccion, curso y estado, combinables entre sí (AND).
     * Cada filtro es opcional (parámetro null = sin filtro). Sin
     * paginación ni orden configurable (no documentados). Orden estable
     * por id (creación), patrón de Horarios (5.4).
     */
    @Query("""
            SELECT s
            FROM SesionClase s
            WHERE (CAST(:fecha AS date) IS NULL OR s.fecha = :fecha)
              AND (:docenteId IS NULL OR s.horario.docente.id = :docenteId)
              AND (:seccionId IS NULL OR s.horario.seccion.id = :seccionId)
              AND (:cursoId IS NULL OR s.horario.curso.id = :cursoId)
              AND (:estado IS NULL OR s.estado = :estado)
            ORDER BY s.id ASC
            """)
    List<SesionClase> buscarConFiltros(@Param("fecha") LocalDate fecha,
                                       @Param("docenteId") Long docenteId,
                                       @Param("seccionId") Long seccionId,
                                       @Param("cursoId") Long cursoId,
                                       @Param("estado") SesionClaseEstado estado);
}