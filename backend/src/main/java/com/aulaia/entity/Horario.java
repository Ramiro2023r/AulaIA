package com.aulaia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Plantilla semanal que define cuándo se dicta un curso para una sección
 * determinada (tabla {@code horarios}, 04-BD §6.7).
 *
 * <p>Validaciones oficiales (04-BD §6.7 y 07-PLAN Prompt 5.2):
 * <ul>
 *   <li>{@code dia_semana} entre 1 y 7, con convención documentada
 *       (1 = Lunes … 7 = Domingo).</li>
 *   <li>{@code hora_fin > hora_inicio} (regla entre dos campos, vía
 *       {@link #esHorarioValido()}); no se soportan horarios que crucen la
 *       medianoche (no documentados).</li>
 *   <li>{@code tolerancia_minutos >= 0} y {@code minutos_antes_apertura
 *       >= 0}.</li>
 * </ul>
 * La BD replica las mismas reglas con CHECK físicos (barrera final).
 *
 * <p>Relaciones obligatorias Curso/Seccion/Docente ({@code @ManyToOne}
 * unidireccionales, LAZY, sin CascadeType.REMOVE). Sin {@code @Data};
 * {@link #toString()} no incluye relaciones (evita ciclos y cargas LAZY).
 * La detección de conflictos entre horarios NO pertenece a esta entidad
 * (Prompt 5.3).
 */
@Entity
@Table(name = "horarios")
@Getter
@Setter
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "curso es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @NotNull(message = "seccion es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @NotNull(message = "docente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    @Min(value = 1, message = "diaSemana debe estar entre 1 y 7")
    @Max(value = 7, message = "diaSemana debe estar entre 1 y 7")
    @Column(name = "dia_semana", nullable = false)
    private short diaSemana;

    @NotNull(message = "horaInicio es obligatoria")
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @NotNull(message = "horaFin es obligatoria")
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Min(value = 0, message = "toleranciaMinutos no puede ser negativa")
    @Column(name = "tolerancia_minutos", nullable = false)
    private short toleranciaMinutos = 10;

    @Min(value = 0, message = "minutosAntesApertura no puede ser negativa")
    @Column(name = "minutos_antes_apertura", nullable = false)
    private short minutosAntesApertura = 15;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Regla oficial entre dos campos (04-BD §6.7, 07-PLAN 5.2):
     * {@code hora_fin > hora_inicio}. Reusable por el futuro Service
     * (Prompt 5.4). Con alguna hora nula devuelve true: la obligatoriedad
     * la cubren {@code @NotNull} por separado.
     */
    @AssertTrue(message = "horaFin debe ser posterior a horaInicio")
    public boolean isHorarioValido() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Override
    public String toString() {
        return "Horario{id=" + id
                + ", diaSemana=" + diaSemana
                + ", horaInicio=" + horaInicio
                + ", horaFin=" + horaFin
                + ", toleranciaMinutos=" + toleranciaMinutos
                + ", minutosAntesApertura=" + minutosAntesApertura
                + ", activo=" + activo + '}';
    }
}