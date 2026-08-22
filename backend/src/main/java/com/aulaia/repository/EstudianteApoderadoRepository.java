package com.aulaia.repository;

import com.aulaia.entity.EstudianteApoderado;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudianteApoderadoRepository extends JpaRepository<EstudianteApoderado, Long> {
    List<EstudianteApoderado> findByEstudianteId(Long estudianteId);

    @Query("select ea from EstudianteApoderado ea join fetch ea.apoderado where ea.estudiante.id = :estudianteId")
    List<EstudianteApoderado> findWithApoderadoByEstudianteId(@Param("estudianteId") Long estudianteId);

    boolean existsByEstudianteIdAndApoderadoId(Long estudianteId, Long apoderadoId);
}
