package com.aulaia.repository;

import com.aulaia.entity.Apoderado;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {

    @Query("""
            select a from Apoderado a
            where a.activo = true
              and (:buscar is null
                   or lower(a.nombres) like lower(concat('%', :buscar, '%'))
                   or lower(a.apellidos) like lower(concat('%', :buscar, '%'))
                   or lower(concat(concat(a.nombres, ' '), a.apellidos)) like lower(concat('%', :buscar, '%'))
                   or lower(coalesce(a.telefono, '')) like lower(concat('%', :buscar, '%')))
              and not exists (
                   select 1 from EstudianteApoderado ea
                   where ea.estudiante.id = :estudianteId and ea.apoderado.id = a.id
              )
            order by a.apellidos asc, a.nombres asc
            """)
    List<Apoderado> buscarActivosNoAsociados(@Param("estudianteId") Long estudianteId,
                                              @Param("buscar") String buscar,
                                              Pageable pageable);
}
