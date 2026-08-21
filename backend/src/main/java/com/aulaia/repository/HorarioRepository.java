package com.aulaia.repository;

import com.aulaia.entity.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

/**
 * Acceso a datos de {@link Horario} (tabla {@code horarios}, 04-BD §6.7).
 *
 * <p>Prompt 5.3: consultas de conflicto de horario (mismo docente y misma
 * sección en horarios solapados del mismo día, 07-PLAN Prompt 5.3 y
 * 06-FLUJOS #11). La matemática del solapamiento es la definida por el
 * prompt (intervalos medio abiertos): {@code existing.horaInicio <
 * nueva.horaFin AND existing.horaFin > nueva.horaInicio}, lo que permite
 * horarios consecutivos (08:00-09:00 y 09:00-10:00 no se solapan).
 *
 * <p>Solo participan horarios activos ({@code activo = true}): la semántica
 * de un horario inactivo no está documentada en 04-BD §6.7; se aplica la
 * convención funcional del sistema (lo inactivo no participa en flujos
 * operativos, cf. estudiantes activos en 04-BD), reportada como decisión
 * técnica en el Prompt 5.3.
 *
 * <p>{@code excluirId} permite actualizaciones sin conflicto consigo mismo:
 * null en creación, el id del horario editado en actualización.
 */
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    List<Horario> findAllByOrderByIdAsc();

    /**
     * ¿Existe otro horario del mismo docente, mismo día y solapado con
     * [horaInicio, horaFin)? Devuelve false si solo coincide el propio
     * horario (excluirId) o si todos los coincidentes están inactivos.
     */
    @Query("""
            SELECT COUNT(h) > 0
            FROM Horario h
            WHERE h.docente.id = :docenteId
              AND h.diaSemana = :diaSemana
              AND h.horaInicio < :horaFin
              AND h.horaFin > :horaInicio
              AND h.activo = true
              AND (:excluirId IS NULL OR h.id <> :excluirId)
            """)
    boolean existeConflictoDocente(@Param("docenteId") Long docenteId,
                                   @Param("diaSemana") Short diaSemana,
                                   @Param("horaInicio") LocalTime horaInicio,
                                   @Param("horaFin") LocalTime horaFin,
                                   @Param("excluirId") Long excluirId);

    /**
     * ¿Existe otro horario de la misma sección, mismo día y solapado con
     * [horaInicio, horaFin)? Misma regla que {@link #existeConflictoDocente}.
     */
    @Query("""
            SELECT COUNT(h) > 0
            FROM Horario h
            WHERE h.seccion.id = :seccionId
              AND h.diaSemana = :diaSemana
              AND h.horaInicio < :horaFin
              AND h.horaFin > :horaInicio
              AND h.activo = true
              AND (:excluirId IS NULL OR h.id <> :excluirId)
            """)
    boolean existeConflictoSeccion(@Param("seccionId") Long seccionId,
                                   @Param("diaSemana") Short diaSemana,
                                   @Param("horaInicio") LocalTime horaInicio,
                                   @Param("horaFin") LocalTime horaFin,
                                   @Param("excluirId") Long excluirId);

    /**
     * Listado con los filtros documentados del Prompt 5.4 (07-PLAN):
     * docente, seccion, curso y dia, combinables entre sí (AND). Cada
     * filtro es opcional (parámetro null = sin filtro). Sin paginación
     * (no documentada en 07-PLAN 5.4; la TRD solo menciona "lista/paginado"
     * como respuesta genérica). Orden estable por id (creación).
     *
     * <p>Para DOCENTE el Service fuerza siempre su propio docenteId: el
     * parámetro docenteId nunca proviene de la sesión como autorización.
     */
    @Query("""
            SELECT h
            FROM Horario h
            WHERE (:docenteId IS NULL OR h.docente.id = :docenteId)
              AND (:seccionId IS NULL OR h.seccion.id = :seccionId)
              AND (:cursoId IS NULL OR h.curso.id = :cursoId)
              AND (:diaSemana IS NULL OR h.diaSemana = :diaSemana)
            ORDER BY h.id ASC
            """)
    List<Horario> buscarConFiltros(@Param("docenteId") Long docenteId,
                                   @Param("seccionId") Long seccionId,
                                   @Param("cursoId") Long cursoId,
                                   @Param("diaSemana") Short diaSemana);
}
