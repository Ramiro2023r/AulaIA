package com.aulaia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Curso del catálogo académico (tabla {@code cursos},
 * docs/04-BASE_DE_DATOS §6.6).
 *
 * <p>Modelo oficial: {@code id, nombre, descripcion (nullable), activo
 * (default TRUE), created_at, updated_at}. Sin UNIQUE, sin CHECK, sin
 * relaciones: el modelo no define restricciones ni FKs en esta tabla, por
 * lo que dos cursos pueden tener el mismo nombre (la BD lo permite y el
 * servicio no agrega restricciones no documentadas).
 *
 * <p>Como en {@link Seccion}, el modelo oficial SÍ define {@code updated_at};
 * se mantiene con callbacks JPA ({@link #preUpdate()}).
 *
 * <p>Se sigue el mismo estilo que {@link Usuario}/{@link Grado}/{@link
 * Seccion}: sin {@code @Data}; {@code toString} manual y sin datos
 * sensibles.
 */
@Entity
@Table(name = "cursos")
@Getter
@Setter
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

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
        return "Curso{id=" + id
                + ", nombre='" + nombre + '\''
                + ", activo=" + activo + '}';
    }
}