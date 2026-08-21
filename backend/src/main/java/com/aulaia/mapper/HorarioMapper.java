package com.aulaia.mapper;

import com.aulaia.dto.horario.HorarioRequest;
import com.aulaia.dto.horario.HorarioResponse;
import com.aulaia.entity.Curso;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Horario;
import com.aulaia.entity.Seccion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeo {@link Horario} ↔ DTOs (MapStruct, componentModel spring por
 * configuración global en {@code pom.xml}).
 *
 * <p>{@code toEntity} ignora las relaciones (el Service las resuelve desde
 * los ids del request, validando existencia, Prompt 5.4 §5/§19),
 * {@code activo} (default TRUE del modelo oficial 04-BD §6.7; la API no
 * administra activo), timestamps e id.
 *
 * <p>{@code toResponse} expone resúmenes mínimos de Curso/Seccion/Docente;
 * nunca entidades JPA ni datos sensibles.
 */
@Mapper
public interface HorarioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "curso", ignore = true)
    @Mapping(target = "seccion", ignore = true)
    @Mapping(target = "docente", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Horario toEntity(HorarioRequest request);

    @Mapping(target = "curso", source = "curso")
    @Mapping(target = "seccion", source = "seccion")
    @Mapping(target = "docente", source = "docente")
    HorarioResponse toResponse(Horario horario);

    HorarioResponse.CursoResumen toCursoResumen(Curso curso);

    HorarioResponse.SeccionResumen toSeccionResumen(Seccion seccion);

    HorarioResponse.DocenteResumen toDocenteResumen(Docente docente);
}