package com.aulaia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Cuenta de acceso de administradores y docentes (tabla {@code usuarios}).
 *
 * <p>Seguridad:
 * <ul>
 *   <li>{@code passwordHash} nunca almacena contraseña plana y se excluye
 *       de {@link #toString()} (y de cualquier serialización accidental
 *       hasta que existan endpoints DTO en prompts posteriores).</li>
 *   <li>No se usa {@code @Data}: equals/hashCode quedan por identidad de
 *       objeto y el {@code toString} es manual y controlado.</li>
 * </ul>
 *
 * <p>Estrategia de timestamps (Prompt 2.1): <b>callbacks JPA</b>
 * ({@code @PrePersist}/{@code @PreUpdate}) como mecanismo único para
 * {@code createdAt}/{@code updatedAt}. La base de datos conserva
 * {@code DEFAULT CURRENT_TIMESTAMP} como respaldo del esquema, pero la
 * aplicación siempre fija ambos valores, garantizando consistencia en
 * todos los perfiles (dev/test/itest).
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "ultimo_login_at")
    private OffsetDateTime ultimoLoginAt;

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
        return "Usuario{id=" + id
                + ", username='" + username + '\''
                + ", rol=" + rol
                + ", activo=" + activo + '}';
    }
}