package com.aulaia.mapper;

import com.aulaia.dto.seccion.SeccionRequest;
import com.aulaia.dto.seccion.SeccionResponse;
import com.aulaia.entity.Seccion;
import org.mapstruct.Mapper;

/**
 * Mapeo {@link Seccion} ↔ DTOs (MapStruct, componentModel spring por
 * configuración global en {@code pom.xml}).
 *
 * <p>{@code toEntity} ignora {@code activo}, {@code createdAt} y
 * {@code updatedAt}: la sección se crea activa y con los defaults del
 * modelo oficial (04-BD §6.4); {@code grado} lo asigna el servicio tras
 * validar su existencia. {@code id} queda null hasta persistir.
 *
 * <p>{@code toResponse} mapea el grado a {@link SeccionResponse.GradoResumen}
 * (solo id y nombre), sin cargar datos innecesarios de {@code Grado}.
 */
@Mapper
public interface SeccionMapper {

    Seccion toEntity(SeccionRequest request);

    SeccionResponse toResponse(Seccion seccion);
}
