package com.aulaia.repository.specification;

import com.aulaia.entity.Asistencia;
import com.aulaia.entity.EstadoAsistencia;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Specifications dinámicas para la consulta de Asistencias (Prompt 7.6).
 */
public class AsistenciaSpecification {

    public static Specification<Asistencia> porFecha(LocalDate fecha) {
        return (root, query, cb) -> {
            if (fecha == null) return null;
            OffsetDateTime inicioDia = fecha.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime finDia = fecha.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            return cb.between(root.get("fechaHora"), inicioDia, finDia);
        };
    }

    public static Specification<Asistencia> porEstado(EstadoAsistencia estado) {
        return (root, query, cb) -> estado == null ? null : cb.equal(root.get("estado"), estado);
    }

    public static Specification<Asistencia> porSeccion(Long seccionId) {
        return (root, query, cb) -> seccionId == null ? null :
                cb.equal(root.join("sesionClase").join("horario").join("seccion").get("id"), seccionId);
    }

    public static Specification<Asistencia> porCurso(Long cursoId) {
        return (root, query, cb) -> cursoId == null ? null :
                cb.equal(root.join("sesionClase").join("horario").join("curso").get("id"), cursoId);
    }

    public static Specification<Asistencia> porEstudiante(Long estudianteId) {
        return (root, query, cb) -> estudianteId == null ? null : cb.equal(root.join("estudiante").get("id"), estudianteId);
    }

    public static Specification<Asistencia> porDocente(Long docenteId) {
        return (root, query, cb) -> docenteId == null ? null :
                cb.equal(root.join("sesionClase").join("horario").join("docente").get("id"), docenteId);
    }
    
    public static Specification<Asistencia> porSesion(Long sesionId) {
        return (root, query, cb) -> sesionId == null ? null : cb.equal(root.join("sesionClase").get("id"), sesionId);
    }
}
