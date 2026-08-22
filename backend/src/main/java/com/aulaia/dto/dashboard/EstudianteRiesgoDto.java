package com.aulaia.dto.dashboard;

public record EstudianteRiesgoDto(
        Long estudianteId,
        String estudianteNombre,
        String cursoNombre,
        String seccionNombre,
        int cantidadFaltas,
        double porcentajeAsistencia
) {}
