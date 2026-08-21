package com.aulaia.dto.dashboard;

import com.aulaia.dto.sesion.SesionClaseResponse;
import java.util.List;

public record DashboardDocenteResponse(
        SesionClaseResponse claseActual,
        List<SesionClaseResponse> clasesDelDia,
        EstadisticasAsistencia estadisticas
) {}
