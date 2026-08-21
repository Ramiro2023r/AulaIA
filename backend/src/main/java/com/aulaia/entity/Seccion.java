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
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Sección académica dentro de un grado y periodo (tabla {@code secciones},
 * docs/04-BASE_DE_DATOS §6.4).
 *
 * <p>Modelo oficial: {@code id, grado_id (FK), nombre, periodo_academico,
 * activo (default TRUE), created_at, updated_at}. A diferencia de
 * {@link Grado}, el modelo oficial SÍ define {@code updated_at} para esta
 * tabla; se mantiene con callbacks JPA ({@link #preUpdate()}).
 *
 * <p>Restricción única oficial {@code uq_seccion_grado_periodo}: dentro del
 * mismo grado y mismo periodo académico no pueden existir dos secciones con
 * el mismo nombre (se aplica en BD y se replica en el servicio).
 *
 * <p>Se sigue el mismo estilo que {@link Usuario}/{@link Grado}: sin
 * {@code @Data}; {@code toString} manual y sin datos sensibles. La relación
 * a {@link Grado} es {@code @ManyToOne LAZY} y unidireccional (no se agrega
 * {@code List<Seccion>} en {@code Grado}, no es necesaria en esta tarea).
 */
@Entity
@Table(name = "secciones")
@Getter
@Setter
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grado_id", nullable = false)
    private Grado grado;

    @Column(nullable = false, length = 20)
    private String nombre;

    @Column(name = "periodo_academico", nullable = false, length = 20)
    private String periodoAcademico;

    @Column(nullable = false)
    private boolean activo = true;

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
        return "Seccion{id=" + id
                + ", nombre='" + nombre + '\''
                + ", periodoAcademico='" + periodoAcademico + '\''
                + ", activo=" + activo + '}';
    }
}
