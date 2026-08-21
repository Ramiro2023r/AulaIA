package com.aulaia.dto.sesion;

import com.aulaia.entity.SesionClaseEstado;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Respuesta de una sesión de clase (Prompts 6.3 y 6.4).
 *
 * <p>Campos mínimos respaldados por la documentación (02-TRD §9 resumen de
 * entidad SesionClase: "id, horarioId, fecha, apertura, cierre, estado").
 * La UI del dashboard (01-PRD §6: "Clase actual y estado de la sesión")
 * necesita identificar la clase, por lo que se exponen resúmenes mínimos
 * de Curso/Seccion/Docente (patrón {@code HorarioResponse}); nunca una
 * entidad JPA ni datos sensibles.
 */
public record SesionClaseResponse(
        Long id,
        Long horarioId,
        LocalDate fecha,
        SesionClaseEstado estado,
        OffsetDateTime horaApertura,
        OffsetDateTime horaCierre,
        java.time.LocalTime horaInicio,
        java.time.LocalTime horaFin,
        CursoResumen curso,
        SeccionResumen seccion,
        DocenteResumen docente) {

    public record CursoResumen(Long id, String nombre) {
    }

    public record SeccionResumen(Long id, String nombre) {
    }

    public record DocenteResumen(Long id, String nombres, String apellidos) {
    }
}