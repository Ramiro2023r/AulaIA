package com.aulaia.repository;

import com.aulaia.entity.EstadoVinculacion;
import com.aulaia.entity.TelegramVinculacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelegramVinculacionRepository extends JpaRepository<TelegramVinculacion, Long> {
    Optional<TelegramVinculacion> findByToken(String token);

    /**
     * Obtiene la invitación bloqueando su fila durante el consumo del token.
     * Así dos procesos de polling no pueden marcar como vinculada la misma
     * invitación de manera concurrente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tv from TelegramVinculacion tv where tv.token = :token")
    Optional<TelegramVinculacion> findByTokenForUpdate(@Param("token") String token);

    List<TelegramVinculacion> findByEstudianteIdAndEstado(Long estudianteId, EstadoVinculacion estado);
}
