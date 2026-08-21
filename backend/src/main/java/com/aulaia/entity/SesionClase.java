package com.aulaia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Ocurrencia real de un horario (plantilla semanal) en una fecha
 * específica (tabla {@code sesiones_clase}, 04-BD §7.1).
 *
 * <p>Modelo oficial (04-BD §7.1): horario obligatorio ({@code @ManyToOne}
 * unidireccional LAZY, sin CascadeType.REMOVE), fecha obligatoria
 * ({@code DATE} → {@link LocalDate}, sin zona horaria), hora de apertura y
 * cierre {@code TIMESTAMPTZ} → {@link OffsetDateTime} NULL (antes de abrir
 * / cerrar), estado {@code VARCHAR(30)} con {@code DEFAULT 'PROGRAMADA'}
 * (04-BD §7.1). La UNIQUE física {@code uq_sesion_horario_fecha
 * (horario_id, fecha)} está en V9; esta entidad no la replica en Java.
 *
 * <p>Prompt 6.1: solo persistencia/modelo. Sin lógica de apertura/cierre,
 * sin cálculo automático de {@code horaApertura}/{@code horaCierre}, sin
 * transiciones de estado (pertenecen a prompts posteriores). No se crea
 * relación inversa en {@link Horario} por iniciativa propia.
 *
 * <p>Sin {@code @Data}: equals/hashCode por identidad de objeto y
 * {@link #toString()} manual que NO incluye el horario (evita ciclos y
 * cargas LAZY accidentales). Estrategia de timestamps (Prompt 2.1):
 * callbacks JPA ({@code @PrePersist}/{@code @PreUpdate}); la BD conserva
 * los defaults como respaldo del esquema.
 */
@Entity
@Table(name = "sesiones_clase")
@Getter
@Setter
public class SesionClase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "horario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horario_id", nullable = false)
    private Horario horario;

    @NotNull(message = "fecha es obligatoria")
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_apertura")
    private OffsetDateTime horaApertura;

    @Column(name = "hora_cierre")
    private OffsetDateTime horaCierre;

    @NotNull(message = "estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private SesionClaseEstado estado = SesionClaseEstado.PROGRAMADA;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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
        return "SesionClase{id=" + id
                + ", fecha=" + fecha
                + ", estado=" + estado + '}';
    }
}