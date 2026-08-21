package com.aulaia.dto.asistencia;

import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.MetodoRegistro;

import java.time.OffsetDateTime;

/**
 * DTO para lectura y consulta de asistencias (Prompt 7.6).
 */
public record AsistenciaResponse(
        Long id,
        Long sesionId,
        Long estudianteId,
        String estudianteNombre,
        String estudianteApellido,
        OffsetDateTime fechaHora,
        EstadoAsistencia estado,
        MetodoRegistro metodo,
        String observacion
) {}
