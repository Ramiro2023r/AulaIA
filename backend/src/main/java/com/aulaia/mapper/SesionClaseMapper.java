package com.aulaia.mapper;

import com.aulaia.dto.sesion.SesionClaseResponse;
import com.aulaia.entity.Curso;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.SesionClase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeo {@link SesionClase} → DTOs (MapStruct, componentModel spring por
 * configuración global en {@code pom.xml}).
 *
 * <p>{@code toResponse} expone el resumen mínimo documentado de sesión
 * (02-TRD §9): id, horarioId, fecha, estado, horaApertura, horaCierre, más
 * resúmenes mínimos de Curso/Seccion/Docente (patrón HorarioResponse, la
 * UI identifica la clase). El mapeo se ejecuta dentro de la transacción
 * del Service (sin OSIV); nunca entidades JPA completas.
 */
@Mapper
public interface SesionClaseMapper {

    @Mapping(target = "horarioId", source = "horario.id")
    @Mapping(target = "curso", source = "horario.curso")
    @Mapping(target = "seccion", source = "horario.seccion")
    @Mapping(target = "docente", source = "horario.docente")
    @Mapping(target = "horaInicio", source = "horario.horaInicio")
    @Mapping(target = "horaFin", source = "horario.horaFin")
    SesionClaseResponse toResponse(SesionClase sesion);

    SesionClaseResponse.CursoResumen toCursoResumen(Curso curso);

    SesionClaseResponse.SeccionResumen toSeccionResumen(Seccion seccion);

    SesionClaseResponse.DocenteResumen toDocenteResumen(Docente docente);
}