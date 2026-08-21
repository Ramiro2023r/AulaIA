package com.aulaia.mapper;

import com.aulaia.dto.curso.CursoRequest;
import com.aulaia.dto.curso.CursoResponse;
import com.aulaia.entity.Curso;
import org.mapstruct.Mapper;

/**
 * Mapeo {@link Curso} ↔ DTOs (MapStruct, componentModel spring por
 * configuración global en {@code pom.xml}).
 *
 * <p>{@code toEntity} ignora {@code activo}, {@code createdAt} y
 * {@code updatedAt}: el curso se crea activo y con los defaults del
 * modelo oficial (04-BD §6.6); {@code id} queda null hasta persistir.
 *
 * <p>{@code toResponse} mapea los campos de API sin información interna.
 */
@Mapper
public interface CursoMapper {

    Curso toEntity(CursoRequest request);

    CursoResponse toResponse(Curso curso);
}