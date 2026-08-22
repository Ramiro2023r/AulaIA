package com.aulaia.repository;

import com.aulaia.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

/**
 * Repositorio de persistencia para {@link Asistencia}
 * (tabla {@code asistencias}, 04-BD §8.1).
 *
 * <p>Prompt 7.1: modelo/unicidad.
 * Prompt 7.3: métodos de verificación de duplicado (ATTENDANCE_ALREADY_REGISTERED)
 * y búsqueda para el flujo de registro (07-PLAN 7.3 paso 5).
 *
 * <p>La restricción UNIQUE física {@code uq_asistencia_sesion_estudiante}
 * (V10) es la barrera final ante concurrencia (Prompt 7.5);
 * el Service verifica primero en modo optimista para devolver errores de
 * negocio antes del flush de Hibernate.
 */
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long>, JpaSpecificationExecutor<Asistencia> {

    /**
     * Verifica si ya existe una asistencia para el par sesión+estudiante.
     * Usado para detectar duplicado antes del save (Prompt 7.3 paso 5).
     */
    boolean existsBySesionClaseIdAndEstudianteId(Long sesionClaseId, Long estudianteId);

    /**
     * Obtiene la asistencia de un estudiante en una sesión determinada.
     * Útil para responder de forma idempotente en caso de duplicado.
     */
    Optional<Asistencia> findBySesionClaseIdAndEstudianteId(Long sesionClaseId, Long estudianteId);

    /**
     * Obtiene todas las asistencias de una sesión.
     */
    List<Asistencia> findBySesionClaseId(Long sesionClaseId);

    @Query("SELECT a FROM Asistencia a WHERE a.sesionClase.horario.docente.id = :docenteId ORDER BY a.fechaHora DESC")
    List<Asistencia> findUltimasAsistenciasPorDocente(@Param("docenteId") Long docenteId, Pageable pageable);

    @Query("SELECT a.estudiante.id, a.estudiante.nombres, a.estudiante.apellidos, a.sesionClase.horario.curso.nombre, a.sesionClase.horario.seccion.nombre, COUNT(a) " +
           "FROM Asistencia a WHERE a.sesionClase.horario.docente.id = :docenteId AND a.estado = 'AUSENTE' " +
           "GROUP BY a.estudiante.id, a.estudiante.nombres, a.estudiante.apellidos, a.sesionClase.horario.curso.nombre, a.sesionClase.horario.seccion.nombre " +
           "HAVING COUNT(a) >= 3 ORDER BY COUNT(a) DESC")
    List<Object[]> findEstudiantesEnRiesgo(@Param("docenteId") Long docenteId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Asistencia a WHERE CAST(a.fechaHora AS date) = CURRENT_DATE")
    long countAsistenciasHoy();

    @Query("SELECT a.estado, COUNT(a) FROM Asistencia a WHERE CAST(a.fechaHora AS date) = CURRENT_DATE GROUP BY a.estado")
    List<Object[]> countDistribucionEstadoHoy();

    @Query("SELECT CAST(a.fechaHora AS date) as fecha, COUNT(a) FROM Asistencia a WHERE a.fechaHora >= :startDate AND a.estado = 'PRESENTE' GROUP BY CAST(a.fechaHora AS date) ORDER BY fecha")
    List<Object[]> countTendenciaAsistenciaDesde(@Param("startDate") java.time.OffsetDateTime startDate);
}
