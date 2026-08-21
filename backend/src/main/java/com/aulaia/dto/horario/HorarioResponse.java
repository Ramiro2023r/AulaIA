package com.aulaia.dto.horario;

import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Respuesta de un horario (Prompt 5.4).
 *
 * <p>Sin entidades JPA: las relaciones se exponen como resúmenes mínimos
 * (patrón del proyecto: {@code SeccionResumen} en estudiantes,
 * {@code UsuarioResumen} en docentes). Sin datos sensibles ni usuarios
 * completos.
 */
public record HorarioResponse(
        Long id,
        short diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        short toleranciaMinutos,
        short minutosAntesApertura,
        boolean activo,
        CursoResumen curso,
        SeccionResumen seccion,
        DocenteResumen docente,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public record CursoResumen(Long id, String nombre) {
    }

    public record SeccionResumen(Long id, String nombre) {
    }

    public record DocenteResumen(Long id, String nombres, String apellidos, boolean activo) {
    }
}