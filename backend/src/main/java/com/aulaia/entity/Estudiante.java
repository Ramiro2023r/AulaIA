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
 * Estudiante registrado en el sistema (tabla {@code estudiantes},
 * docs/04-BASE_DE_DATOS §6.5).
 *
 * <p>Modelo oficial: {@code id, codigo (UNIQUE), qr_token (UNIQUE),
 * nombres, apellidos, seccion_id (FK a secciones), activo (default TRUE),
 * created_at, updated_at}. El documento define {@code updated_at}, por lo
 * que se mantiene con callbacks JPA ({@link #preUpdate()}), como
 * {@link Seccion} y {@link Curso}.
 *
 * <p>{@code codigo} y {@code qr_token} son UNIQUE a nivel BD (única
 * restricción de unicidad documentada); nombres/apellidos no son únicos.
 * En este prompt el token es solo un campo persistente único: la
 * generación segura corresponde al Prompt 4.2.
 *
 * <p>Se sigue el mismo estilo que {@link Usuario}/{@link Grado}/
 * {@link Seccion}/{@link Curso}: sin {@code @Data}; {@code toString}
 * manual y sin la relación {@code Seccion} (evita
 * {@code LazyInitializationException} y ciclos). La relación a
 * {@link Seccion} es {@code @ManyToOne LAZY} y unidireccional (no se
 * agrega {@code List<Estudiante>} en {@code Seccion}, no es necesaria en
 * esta tarea).
 */
@Entity
@Table(name = "estudiantes")
@Getter
@Setter
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String codigo;

    @Column(name = "qr_token", nullable = false, length = 120, unique = true)
    private String qrToken;

    @Column(nullable = false, length = 120)
    private String nombres;

    @Column(nullable = false, length = 120)
    private String apellidos;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

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
        return "Estudiante{id=" + id
                + ", codigo='" + codigo + '\''
                + ", activo=" + activo + '}';
    }
}