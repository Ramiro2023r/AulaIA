package com.aulaia.dto.dashboard;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;
import com.aulaia.dto.justificacion.JustificacionResponse;

@Data
@Builder
public class AdminDashboardResponse {
    private long totalEstudiantes;
    private long totalDocentes;
    private long totalSecciones;
    private double asistenciaHoyPorcentaje;
    
    private Map<String, Integer> distribucionEstadoHoy;
    private List<TendenciaAsistenciaDto> tendencia7Dias;
    private List<JustificacionResponse> justificacionesPendientes;
}
