package com.aulaia.dto.dashboard;

import com.aulaia.dto.sesion.SesionClaseResponse;
import java.util.List;

public record DashboardDocenteResponse(
        SesionClaseResponse claseActual,
        Integer claseActualAsistentes,
        Integer claseActualTotalEstudiantes,
        List<SesionClaseResponse> clasesDelDia,
        EstadisticasAsistencia estadisticas,
        List<EstudianteRiesgoDto> estudiantesRiesgo,
        List<AsistenciaRecienteDto> ultimosRegistros
) {}
