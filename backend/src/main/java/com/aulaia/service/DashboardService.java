package com.aulaia.service;

import com.aulaia.dto.dashboard.AdminDashboardResponse;
import com.aulaia.dto.dashboard.TendenciaAsistenciaDto;
import com.aulaia.dto.justificacion.JustificacionResponse;
import com.aulaia.mapper.JustificacionMapper;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.JustificacionRepository;
import com.aulaia.repository.SeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final SeccionRepository seccionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final JustificacionRepository justificacionRepository;
    private final JustificacionMapper justificacionMapper;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboardMetrics() {
        long totalEstudiantes = estudianteRepository.count();
        long totalDocentes = docenteRepository.count();
        long totalSecciones = seccionRepository.count();

        long asistenciasHoy = asistenciaRepository.countAsistenciasHoy();
        double porcentajeHoy = 0.0;
        
        // Calculate percentages based on today's total registered attendances
        Map<String, Integer> distribucionEstadoHoy = new HashMap<>();
        List<Object[]> distRows = asistenciaRepository.countDistribucionEstadoHoy();
        for (Object[] row : distRows) {
            String estado = row[0].toString();
            Long count = (Long) row[1];
            distribucionEstadoHoy.put(estado, count.intValue());
        }

        if (asistenciasHoy > 0) {
            int presentes = distribucionEstadoHoy.getOrDefault("PRESENTE", 0);
            porcentajeHoy = (double) presentes / asistenciasHoy * 100.0;
            porcentajeHoy = Math.round(porcentajeHoy * 10.0) / 10.0; // round to 1 decimal
        }

        // Tendencia de los últimos 7 días
        java.time.OffsetDateTime sevenDaysAgo = java.time.OffsetDateTime.now().minusDays(6).with(java.time.LocalTime.MIN);
        List<Object[]> tendenciaRows = asistenciaRepository.countTendenciaAsistenciaDesde(sevenDaysAgo);
        
        // Here we just map the raw count of 'PRESENTE' to a percentage metric (assuming max ~ totalEstudiantes)
        // Ideally we'd calculate total attendances per day, but for simplicity we'll show raw counts
        List<TendenciaAsistenciaDto> tendencia7Dias = tendenciaRows.stream().map(row -> {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            Long count = (Long) row[1];
            
            // To show a percentage we assume everyone was registered. If we just have the count, we use it as percentage relative to totalEstudiantes
            double perc = totalEstudiantes > 0 ? ((double) count / totalEstudiantes) * 100.0 : 0.0;
            return TendenciaAsistenciaDto.builder()
                .fecha(sqlDate.toLocalDate())
                .porcentajeAsistencia(Math.round(perc * 10.0) / 10.0)
                .build();
        }).collect(Collectors.toList());

        // Pendientes
        var pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        var pendientes = justificacionRepository.findByEstado(com.aulaia.entity.EstadoJustificacion.PENDIENTE, pageRequest)
                .getContent()
                .stream()
                .map(justificacionMapper::toResponse)
                .collect(Collectors.toList());

        return AdminDashboardResponse.builder()
                .totalEstudiantes(totalEstudiantes)
                .totalDocentes(totalDocentes)
                .totalSecciones(totalSecciones)
                .asistenciaHoyPorcentaje(porcentajeHoy)
                .distribucionEstadoHoy(distribucionEstadoHoy)
                .tendencia7Dias(tendencia7Dias)
                .justificacionesPendientes(pendientes)
                .build();
    }
}
