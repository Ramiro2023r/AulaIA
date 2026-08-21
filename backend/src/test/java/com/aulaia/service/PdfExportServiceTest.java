package com.aulaia.service;

import com.aulaia.dto.ReporteAsistenciaDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfExportServiceTest {

    private final PdfExportService pdfExportService = new PdfExportService();

    @Test
    void generarReporteAsistencia_retornaBytes() {
        // Arrange
        ReporteAsistenciaDto dto = ReporteAsistenciaDto.builder()
                .fecha(LocalDate.of(2026, 8, 20))
                .estudianteNombreCompleto("Ana Gómez")
                .cursoNombre("Ciencias")
                .seccionNombre("B")
                .estadoAsistencia("PRESENTE")
                .justificacionEstado("N/A")
                .build();

        List<ReporteAsistenciaDto> datos = List.of(dto);

        // Act
        byte[] resultado = pdfExportService.generarReporteAsistencia(datos);

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.length > 0);
    }
}
