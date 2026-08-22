package com.aulaia.repository;

import com.aulaia.entity.Justificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JustificacionRepository extends JpaRepository<Justificacion, Long> {
    
    Optional<Justificacion> findByAsistenciaId(Long asistenciaId);

    List<Justificacion> findByAsistenciaIdIn(List<Long> asistenciaIds);
    
    org.springframework.data.domain.Page<Justificacion> findByEstado(com.aulaia.entity.EstadoJustificacion estado, org.springframework.data.domain.Pageable pageable);
}
