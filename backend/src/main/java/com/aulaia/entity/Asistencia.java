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
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Registro de asistencia de un estudiante a una sesión de clase
 * (tabla {@code asistencias}, 04-BD §8.1).
 *
 * <p>Modelo oficial (04-BD §8.1):
 * <ul>
 *   <li>{@code sesion_clase_id} → {@link SesionClase} (FK, NOT NULL, LAZY, optional=false)</li>
 *   <li>{@code estudiante_id}   → {@link Estudiante} (FK, NOT NULL, LAZY, optional=false)</li>
 *   <li>{@code fecha_hora}      → {@link OffsetDateTime} (TIMESTAMPTZ, NOT NULL,
 *                                  DEFAULT CURRENT_TIMESTAMP en BD)</li>
 *   <li>{@code estado}          → {@link EstadoAsistencia} (VARCHAR 30, NOT NULL,
 *                                  {@code @Enumerated(STRING)})</li>
 *   <li>{@code metodo}          → {@link MetodoRegistro} (VARCHAR 30, NOT NULL,
 *                                  {@code @Enumerated(STRING)})</li>
 *   <li>{@code observacion}     → String (VARCHAR 500, NULL permitido)</li>
 *   <li>{@code created_at}      → {@link OffsetDateTime} (NOT NULL, solo insert)</li>
 *   <li>{@code updated_at}      → {@link OffsetDateTime} (NOT NULL, actualizado en update)</li>
 * </ul>
 *
 * <p>UNIQUE física {@code uq_asistencia_sesion_estudiante (sesion_clase_id, estudiante_id)}
 * está definida en V10 — impide que el mismo estudiante tenga más de una asistencia
 * por sesión (RF-DATA-001, 04-BD §15). Esta entidad no replica la constraint en JPA.
 *
 * <p>Alcance del Prompt 7.1: solo modelo/persistencia.
 * <ul>
 *   <li>Sin lógica de registro (Prompt 7.3).</li>
 *   <li>Sin validación de sesión ABIERTA (Prompt 7.3).</li>
 *   <li>Sin cálculo PRESENTE/TARDANZA (Prompt 7.3).</li>
 *   <li>Sin resolución QR/código (Prompt 7.2).</li>
 *   <li>Sin generación de AUSENTE (cierre de sesión, sprint posterior).</li>
 *   <li>Sin relación inversa en {@link SesionClase} ni en {@link Estudiante}.</li>
 *   <li>Sin cascade REMOVE.</li>
 * </ul>
 *
 * <p>Sin {@code @Data}: equals/hashCode por identidad de objeto;
 * {@link #toString()} manual que NO incluye sesionClase ni estudiante
 * (evita ciclos y cargas LAZY accidentales).
 * Timestamps gestionados por callbacks JPA ({@code @PrePersist}/{@code @PreUpdate});
 * la BD conserva sus defaults como respaldo físico del esquema.
 */
@Entity
@Table(name = "asistencias")
@Getter
@Setter
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Sesión de clase a la que pertenece esta asistencia.
     * FK {@code fk_asistencias_sesion} → {@code sesiones_clase(id)}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_clase_id", nullable = false)
    private SesionClase sesionClase;

    /**
     * Estudiante al que corresponde esta asistencia.
     * FK {@code fk_asistencias_estudiante} → {@code estudiantes(id)}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    /**
     * Momento exacto del registro de asistencia.
     * Tipo TIMESTAMPTZ en BD (→ OffsetDateTime en Java).
     * La BD asigna DEFAULT CURRENT_TIMESTAMP si no se especifica.
     * La regla "usar hora del servidor al registrar" corresponde al Prompt 7.3.
     */
    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora;

    /**
     * Estado de la asistencia (PRESENTE / TARDANZA / AUSENTE / JUSTIFICADO).
     * Mapeado como STRING — nunca ordinal.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoAsistencia estado;

    /**
     * Método por el cual se registró la asistencia
     * (QR / CODIGO / MANUAL_DOCENTE / SISTEMA).
     * Mapeado como STRING — nunca ordinal.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo", nullable = false, length = 30)
    private MetodoRegistro metodo;

    /**
     * Observación libre, nullable, máximo 500 caracteres.
     * No se impone validación @NotBlank ni longitud mínima (04-BD §8.1).
     */
    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (fechaHora == null) {
            fechaHora = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Override
    public String toString() {
        return "Asistencia{id=" + id
                + ", estado=" + estado
                + ", metodo=" + metodo
                + ", fechaHora=" + fechaHora + '}';
    }
}
