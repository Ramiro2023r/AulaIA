package com.aulaia.mapper;

import com.aulaia.dto.estudiante.EstudianteRequest;
import com.aulaia.dto.estudiante.EstudianteResponse;
import com.aulaia.entity.Estudiante;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeo {@link Estudiante} ↔ DTOs (MapStruct, componentModel spring por
 * configuración global en {@code pom.xml}).
 *
 * <p>{@code toEntity} ignora {@code seccion} (la asigna el Service tras
 * validar su existencia), {@code qrToken} (se genera de forma segura en
 * el Service, Prompt 4.2), {@code activo}, {@code createdAt} y
 * {@code updatedAt}: el estudiante se crea activo con los defaults del
 * modelo oficial (04-BD §6.5); {@code id} queda null hasta persistir.
 *
 * <p>{@code toResponse} mapea la sección a
 * {@link EstudianteResponse.SeccionResumen} (solo id y nombre); el
 * {@code qrToken} no forma parte de la respuesta (opaco, no expuesto).
 */
@Mapper
public interface EstudianteMapper {

    @Mapping(target = "seccion", ignore = true)
    @Mapping(target = "qrToken", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    Estudiante toEntity(EstudianteRequest request);

    EstudianteResponse toResponse(Estudiante estudiante);
}