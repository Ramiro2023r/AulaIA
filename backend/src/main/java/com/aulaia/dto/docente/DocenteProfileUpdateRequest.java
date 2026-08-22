package com.aulaia.dto.docente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para actualizar el perfil del docente")
public record DocenteProfileUpdateRequest(
        @Schema(description = "Correo alternativo", example = "juan.perez@gmail.com")
        @Email(message = "El correo alternativo debe ser válido")
        @Size(max = 100, message = "El correo alternativo no puede exceder los 100 caracteres")
        String correoAlternativo,
        
        @Schema(description = "Teléfono", example = "987654321")
        @Size(max = 20, message = "El teléfono no puede exceder los 20 caracteres")
        String telefono,
        
        @Schema(description = "Biografía corta", example = "Profesor de matemáticas con 10 años de experiencia.")
        @Size(max = 500, message = "La biografía no puede exceder los 500 caracteres")
        String biografia
) {}
