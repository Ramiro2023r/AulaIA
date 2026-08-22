package com.aulaia.repository;

import com.aulaia.entity.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de {@link Docente} (tabla {@code docentes}, 04-BD §6.2).
 *
 * <p>Consultas necesarias para la relación documentada 1 a 1
 * ({@code usuarios 1 ─── 0..1 docentes}): localizar el perfil docente de un
 * usuario y verificar que el usuario aún no tenga uno (validación de la
 * UNIQUE física {@code usuario_id}). Sin búsquedas por nombre: el plan no
 * las pide. Orden estable por id (creación), patrón del proyecto.
 */
public interface DocenteRepository extends JpaRepository<Docente, Long> {

    Optional<Docente> findByUsuarioId(Long usuarioId);

    Optional<Docente> findByUsuarioUsername(String username);

    boolean existsByUsuarioId(Long usuarioId);

    List<Docente> findAllByOrderByIdAsc();
}