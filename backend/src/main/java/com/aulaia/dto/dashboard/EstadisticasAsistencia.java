package com.aulaia.dto.dashboard;

public record EstadisticasAsistencia(
        int presentes,
        int tardanzas,
        int ausentes,
        int totalEstudiantes,
        double porcentajeAsistencia
) {}
