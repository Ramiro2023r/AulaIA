package com.aulaia.service;

import com.aulaia.entity.EstadoAsistencia;

import java.time.OffsetDateTime;

/** Instantánea mínima de una asistencia nueva para notificaciones posteriores al commit. */
public record AsistenciaRegistradaEvent(
        Long asistenciaId,
        Long estudianteId,
        String nombreEstudiante,
        OffsetDateTime fechaHora,
        EstadoAsistencia estado,
        String curso,
        String gradoSeccion) {
}
