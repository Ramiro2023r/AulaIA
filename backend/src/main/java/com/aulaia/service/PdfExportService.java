package com.aulaia.service;

import com.aulaia.dto.ReporteAsistenciaDto;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class PdfExportService {

    public byte[] generarReporteAsistencia(List<ReporteAsistenciaDto> asistencias) {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            fontTitle.setSize(18);
            Paragraph title = new Paragraph("Reporte de Asistencias", fontTitle);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Tabla
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{1.5f, 3.5f, 2f, 1f, 1.5f, 1.5f});

            writeTableHeader(table);
            writeTableData(table, asistencias);

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar reporte PDF", e);
        }

        return out.toByteArray();
    }

    private void writeTableHeader(PdfPTable table) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setPadding(5);

        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        font.setColor(Color.BLACK);

        String[] headers = {"Fecha", "Estudiante", "Curso", "Sección", "Estado Asistencia", "Estado Justificación"};
        for (String header : headers) {
            cell.setPhrase(new Phrase(header, font));
            table.addCell(cell);
        }
    }

    private void writeTableData(PdfPTable table, List<ReporteAsistenciaDto> asistencias) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA);
        for (ReporteAsistenciaDto dto : asistencias) {
            table.addCell(new Phrase(dto.getFecha() != null ? dto.getFecha().toString() : "", font));
            table.addCell(new Phrase(dto.getEstudianteNombreCompleto() != null ? dto.getEstudianteNombreCompleto() : "", font));
            table.addCell(new Phrase(dto.getCursoNombre() != null ? dto.getCursoNombre() : "", font));
            table.addCell(new Phrase(dto.getSeccionNombre() != null ? dto.getSeccionNombre() : "", font));
            table.addCell(new Phrase(dto.getEstadoAsistencia() != null ? dto.getEstadoAsistencia() : "", font));
            table.addCell(new Phrase(dto.getJustificacionEstado() != null ? dto.getJustificacionEstado() : "N/A", font));
        }
    }
}
