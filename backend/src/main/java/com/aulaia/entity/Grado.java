package com.aulaia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Grado escolar (tabla {@code grados}, docs/04-BASE_DE_DATOS §6.3).
 *
 * <p>Modelo oficial: {@code id, nombre, nivel (default 'PRIMARIA'), orden
 * (nullable), activo (default TRUE), created_at}. El documento oficial NO
 * define columna {@code updated_at} para esta tabla, por lo que la entidad
 * tampoco la tiene (la estrategia de timestamps usa callbacks JPA igual que
 * {@link Usuario}, pero solo con {@code createdAt}).
 *
 * <p>Se sigue el mismo estilo que {@link Usuario}: sin {@code @Data} para
 * controlar equals/hashCode/toString (el {@code toString} es manual y sin
 * datos sensibles).
 */
@Entity
@Table(name = "grados")
@Getter
@Setter
public class Grado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String nivel = "PRIMARIA";

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = true)
    private Integer orden;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    @Override
    public String toString() {
        return "Grado{id=" + id
                + ", nombre='" + nombre + '\''
                + ", nivel='" + nivel + '\''
                + ", activo=" + activo + '}';
    }
}