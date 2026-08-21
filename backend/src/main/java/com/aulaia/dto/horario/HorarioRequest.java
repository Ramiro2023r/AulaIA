package com.aulaia.dto.horario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Datos de entrada para crear o actualizar un horario (Prompt 5.4,
 * 07-PLAN: formulario de la UI 05-UI_UX #29 y flujo 06-FLUJOS #11).
 *
 * <p>Contiene únicamente campos administrables: curso, sección, docente,
 * día, horas, tolerancia y minutos antes de apertura. {@code activo} NO
 * forma parte del contrato: ni el formulario (05-UI_UX #29) ni el flujo
 * (06-FLUJOS #11) lo capturan, y no existe operación documentada de
 * activación/desactivación de horarios. Timestamps y entidades completas
 * nunca se aceptan.
 *
 * <p>Validaciones oficiales (04-BD §6.7, 07-PLAN 5.2/5.4): ids de
 * relaciones obligatorios; {@code diaSemana} 1-7 (1 = Lunes … 7 = Domingo);
 * horas obligatorias con {@code horaFin > horaInicio}; tolerancia y
 * apertura >= 0. Sin máximos inventados (VARCHAR/SMALLINT limitan en BD).
 */
public record HorarioRequest(
        @NotNull(message = "cursoId es obligatorio")
        @Schema(description = "Id del curso (debe existir)")
        Long cursoId,

        @NotNull(message = "seccionId es obligatorio")
        @Schema(description = "Id de la sección (debe existir)")
        Long seccionId,

        @NotNull(message = "docenteId es obligatorio")
        @Schema(description = "Id del docente (debe existir)")
        Long docenteId,

        @Min(value = 1, message = "diaSemana debe estar entre 1 y 7")
        @Max(value = 7, message = "diaSemana debe estar entre 1 y 7")
        @Schema(description = "Día de la semana: 1 = Lunes … 7 = Domingo")
        short diaSemana,

        @NotNull(message = "horaInicio es obligatoria")
        @Schema(description = "Hora de inicio (HH:mm)", example = "08:00")
        LocalTime horaInicio,

        @NotNull(message = "horaFin es obligatoria")
        @Schema(description = "Hora de fin, posterior a horaInicio (HH:mm)", example = "09:00")
        LocalTime horaFin,

        @Min(value = 0, message = "toleranciaMinutos no puede ser negativa")
        @Schema(description = "Minutos de tolerancia de llegada (>= 0)", example = "10")
        short toleranciaMinutos,

        @Min(value = 0, message = "minutosAntesApertura no puede ser negativa")
        @Schema(description = "Minutos antes de la apertura de la sesión (>= 0)", example = "15")
        short minutosAntesApertura) {

    /**
     * Regla oficial entre dos campos (04-BD §6.7, 07-PLAN 5.2):
     * {@code horaFin > horaInicio}. Con alguna hora nula devuelve true: la
     * obligatoriedad la cubren los {@code @NotNull} por separado.
     */
    @AssertTrue(message = "horaFin debe ser posterior a horaInicio")
    public boolean isRangoHorarioValido() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}