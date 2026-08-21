package com.aulaia.mapper;

import com.aulaia.dto.justificacion.JustificacionResponse;
import com.aulaia.entity.Justificacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JustificacionMapper {

    @Mapping(target = "asistenciaId", source = "asistencia.id")
    @Mapping(target = "estudianteNombre", source = "asistencia.estudiante.nombres")
    @Mapping(target = "estudianteApellidos", source = "asistencia.estudiante.apellidos")
    @Mapping(target = "cursoNombre", source = "asistencia.sesionClase.horario.curso.nombre")
    @Mapping(target = "fechaSesion", source = "asistencia.sesionClase.fecha")
    @Mapping(target = "estadoAsistencia", source = "asistencia.estado")
    @Mapping(target = "revisadoPorNombre", source = "revisadoPor.username")
    JustificacionResponse toResponse(Justificacion justificacion);

    List<JustificacionResponse> toResponseList(List<Justificacion> justificaciones);
}
