package com.aulaia.mapper;

import com.aulaia.dto.grado.GradoRequest;
import com.aulaia.dto.grado.GradoResponse;
import com.aulaia.entity.Grado;
import org.mapstruct.Mapper;

/**
 * Mapeo {@link Grado} ↔ DTOs (MapStruct, componentModel spring por
 * configuración global en {@code pom.xml}).
 *
 * <p>{@code toEntity} ignora {@code activo} y {@code createdAt}: el grado
 * se crea activo con los defaults del modelo oficial; {@code id} queda
 * null hasta persistir.
 */
@Mapper
public interface GradoMapper {

    Grado toEntity(GradoRequest request);

    GradoResponse toResponse(Grado grado);
}