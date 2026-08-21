package com.aulaia.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ReporteAsistenciaDto {
    private LocalDate fecha;
    private String estudianteNombreCompleto;
    private String cursoNombre;
    private String seccionNombre;
    private String estadoAsistencia;
    private String justificacionEstado;
}
