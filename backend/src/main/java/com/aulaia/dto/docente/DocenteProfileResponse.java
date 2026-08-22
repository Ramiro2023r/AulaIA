package com.aulaia.dto.docente;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta con los datos del perfil del docente")
public record DocenteProfileResponse(
        @Schema(description = "ID del docente", example = "1")
        Long id,
        
        @Schema(description = "Nombres", example = "Juan")
        String nombres,
        
        @Schema(description = "Apellidos", example = "Pérez")
        String apellidos,
        
        @Schema(description = "Correo alternativo", example = "juan.perez@gmail.com")
        String correoAlternativo,
        
        @Schema(description = "Teléfono", example = "987654321")
        String telefono,
        
        @Schema(description = "Biografía corta", example = "Profesor de matemáticas con 10 años de experiencia.")
        String biografia
) {}
