package com.aulaia.dto.estudiante;

/** Información mínima para buscar y asociar un apoderado existente. */
public record ApoderadoDisponibleResponse(
        Long id,
        String nombres,
        String apellidos,
        String telefono) {
}
