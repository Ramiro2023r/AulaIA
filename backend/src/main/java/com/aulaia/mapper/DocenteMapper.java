package com.aulaia.mapper;

import com.aulaia.dto.docente.DocenteRequest;
import com.aulaia.dto.docente.DocenteResponse;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeo {@link Docente} ↔ DTOs (MapStruct, componentModel spring por
 * configuración global en {@code pom.xml}).
 *
 * <p>{@code toEntity} ignora {@code usuario} (lo crea el Service junto con
 * la cuenta DOCENTE, Prompt 5.1), {@code activo} (default TRUE del modelo
 * oficial 04-BD §6.2), timestamps e id.
 *
 * <p>{@code toResponse} mapea el usuario a
 * {@link DocenteResponse.UsuarioResumen} (solo id, username, rol y activo);
 * nunca password ni passwordHash.
 */
@Mapper
public interface DocenteMapper {

    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    Docente toEntity(DocenteRequest request);

    @Mapping(target = "usuario", source = "usuario")
    DocenteResponse toResponse(Docente docente);

    DocenteResponse.UsuarioResumen toUsuarioResumen(Usuario usuario);

    com.aulaia.dto.docente.DocenteProfileResponse toProfileResponse(Docente docente);
}