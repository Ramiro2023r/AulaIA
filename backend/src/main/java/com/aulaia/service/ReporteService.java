package com.aulaia.service;

import com.aulaia.dto.ReporteAsistenciaDto;
import com.aulaia.dto.ReporteFiltrosDto;
import com.aulaia.entity.Asistencia;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.Justificacion;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.JustificacionRepository;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final AsistenciaRepository asistenciaRepository;
    private final JustificacionRepository justificacionRepository;

    @Transactional(readOnly = true)
    public List<ReporteAsistenciaDto> generarReporteAsistencias(ReporteFiltrosDto filtros) {
        log.info("Generando reporte de asistencias con filtros: {}", filtros);

        Specification<Asistencia> spec = buildSpec(filtros);
        List<Asistencia> asistencias = asistenciaRepository.findAll(spec);

        if (asistencias.isEmpty()) {
            return new ArrayList<>();
        }

        // Obtener justificaciones asociadas en bloque para evitar N+1
        List<Long> asistenciaIds = asistencias.stream().map(Asistencia::getId).toList();
        List<Justificacion> justificaciones = justificacionRepository.findByAsistenciaIdIn(asistenciaIds);
        
        Map<Long, String> justificacionEstadoMap = justificaciones.stream()
                .collect(Collectors.toMap(
                        j -> j.getAsistencia().getId(),
                        j -> j.getEstado().name()
                ));

        return asistencias.stream().map(a -> {
            String justEstado = justificacionEstadoMap.get(a.getId());
            return ReporteAsistenciaDto.builder()
                    .asistenciaId(a.getId())
                    .fecha(a.getSesionClase().getFecha())
                    .estudianteId(a.getEstudiante().getId())
                    .estudianteNombreCompleto(a.getEstudiante().getNombres() + " " + a.getEstudiante().getApellidos())
                    .cursoNombre(a.getSesionClase().getHorario().getCurso().getNombre())
                    .seccionNombre(a.getSesionClase().getHorario().getSeccion().getNombre())
                    .estadoAsistencia(a.getEstado().name())
                    .justificacionEstado(justEstado)
                    .build();
        }).toList();
    }

    private Specification<Asistencia> buildSpec(ReporteFiltrosDto filtros) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            // Join a sesionClase
            Join<Object, Object> sesionJoin = root.join("sesionClase");
            Join<Object, Object> horarioJoin = sesionJoin.join("horario");
            Join<Object, Object> estudianteJoin = root.join("estudiante");

            if (filtros.getFechaInicio() != null) {
                predicates.add(cb.greaterThanOrEqualTo(sesionJoin.get("fecha"), filtros.getFechaInicio()));
            }

            if (filtros.getFechaFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(sesionJoin.get("fecha"), filtros.getFechaFin()));
            }

            if (filtros.getCursoId() != null) {
                predicates.add(cb.equal(horarioJoin.join("curso").get("id"), filtros.getCursoId()));
            }

            if (filtros.getSeccionId() != null) {
                predicates.add(cb.equal(horarioJoin.join("seccion").get("id"), filtros.getSeccionId()));
            }

            if (filtros.getEstudianteId() != null) {
                predicates.add(cb.equal(estudianteJoin.get("id"), filtros.getEstudianteId()));
            }

            if (filtros.getDocenteId() != null) {
                predicates.add(cb.equal(horarioJoin.join("docente").get("id"), filtros.getDocenteId()));
            }

            if (filtros.getEstadoAsistencia() != null && !filtros.getEstadoAsistencia().isBlank()) {
                try {
                    EstadoAsistencia estadoEnum = EstadoAsistencia.valueOf(filtros.getEstadoAsistencia());
                    predicates.add(cb.equal(root.get("estado"), estadoEnum));
                } catch (IllegalArgumentException e) {
                    log.warn("Filtro estadoAsistencia inválido: {}", filtros.getEstadoAsistencia());
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
