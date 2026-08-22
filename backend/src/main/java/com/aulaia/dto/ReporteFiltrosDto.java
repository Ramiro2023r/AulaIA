package com.aulaia.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReporteFiltrosDto {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Long cursoId;
    private Long seccionId;
    private Long estudianteId;
    private String estadoAsistencia;
    private Long docenteId;
}
