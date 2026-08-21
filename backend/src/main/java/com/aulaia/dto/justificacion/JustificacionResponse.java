package com.aulaia.dto.justificacion;

import com.aulaia.entity.EstadoJustificacion;

import java.time.OffsetDateTime;

public record JustificacionResponse(
        Long id,
        Long asistenciaId,
        String estudianteNombre,
        String estudianteApellidos,
        String cursoNombre,
        java.time.LocalDate fechaSesion,
        com.aulaia.entity.EstadoAsistencia estadoAsistencia,
        String motivo,
        EstadoJustificacion estado,
        String revisadoPorNombre,
        OffsetDateTime fechaRevision,
        OffsetDateTime createdAt
) {}
