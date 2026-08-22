package com.aulaia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Información académica asociada a un usuario con rol DOCENTE
 * (tabla {@code docentes}, 04-BD §6.2).
 *
 * <p>Relación oficial: {@code usuarios 1 ─── 0..1 docentes} (04-BD §6.2).
 * Unidireccional: {@link Docente} conoce a su {@link Usuario}; {@code Usuario}
 * no tiene referencia hacia Docente (no se crea relación bidireccional por
 * iniciativa propia). La unicidad física de {@code usuario_id} (UNIQUE en V7)
 * garantiza el 1:1.
 *
 * <p>Sin {@code @Data}: equals/hashCode por identidad de objeto y
 * {@link #toString()} manual que NO incluye el usuario (evita ciclos y
 * cargas LAZY accidentales). Sin {@code CascadeType.REMOVE}: los documentos
 * no definen borrado; el historial se mantiene (06-FLUJOS #49).
 *
 * <p>Estrategia de timestamps (Prompt 2.1): callbacks JPA
 * ({@code @PrePersist}/{@code @PreUpdate}); la BD conserva los defaults
 * como respaldo del esquema.
 */
@Entity
@Table(name = "docentes")
@Getter
@Setter
public class Docente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String nombres;

    @Column(nullable = false, length = 120)
    private String apellidos;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "correo_alternativo", length = 100)
    private String correoAlternativo;

    @Column(length = 20)
    private String telefono;

    @Column(columnDefinition = "TEXT")
    private String biografia;

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
        return "Docente{id=" + id
                + ", nombres='" + nombres + '\''
                + ", apellidos='" + apellidos + '\''
                + ", activo=" + activo + '}';
    }
}