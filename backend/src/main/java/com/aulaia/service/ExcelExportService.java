package com.aulaia.service;

import com.aulaia.dto.ReporteAsistenciaDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] generarReporteAsistencia(List<ReporteAsistenciaDto> asistencias) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Reporte de Asistencias");

            // Crear estilo para encabezados
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Crear fila de encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Fecha", "Estudiante", "Curso", "Sección", "Estado Asistencia", "Estado Justificación"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos
            int rowIdx = 1;
            for (ReporteAsistenciaDto dto : asistencias) {
                Row row = sheet.createRow(rowIdx++);
                
                row.createCell(0).setCellValue(dto.getFecha() != null ? dto.getFecha().toString() : "");
                row.createCell(1).setCellValue(dto.getEstudianteNombreCompleto() != null ? dto.getEstudianteNombreCompleto() : "");
                row.createCell(2).setCellValue(dto.getCursoNombre() != null ? dto.getCursoNombre() : "");
                row.createCell(3).setCellValue(dto.getSeccionNombre() != null ? dto.getSeccionNombre() : "");
                row.createCell(4).setCellValue(dto.getEstadoAsistencia() != null ? dto.getEstadoAsistencia() : "");
                row.createCell(5).setCellValue(dto.getJustificacionEstado() != null ? dto.getJustificacionEstado() : "N/A");
            }

            // Autoajustar columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar archivo Excel", e);
        }
    }
}
