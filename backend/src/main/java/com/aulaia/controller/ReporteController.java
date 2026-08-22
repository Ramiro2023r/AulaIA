package com.aulaia.controller;

import com.aulaia.dto.ReporteAsistenciaDto;
import com.aulaia.dto.ReporteFiltrosDto;
import com.aulaia.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.aulaia.service.ExcelExportService;
import com.aulaia.service.PdfExportService;
import com.aulaia.repository.UsuarioRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.entity.Docente;
import com.aulaia.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Endpoints para generación de reportes")
public class ReporteController {

    private final ReporteService reporteService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;

    private void aplicarFiltroDocenteAutenticado(ReporteFiltrosDto filtros) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCENTE"))) {
            if (auth.getPrincipal() instanceof UserDetails principal) {
                Long docenteId = usuarioRepository.findByUsername(principal.getUsername())
                        .flatMap(usuario -> docenteRepository.findByUsuarioId(usuario.getId()))
                        .map(Docente::getId)
                        .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado", "TEACHER_NOT_FOUND"));
                filtros.setDocenteId(docenteId);
            }
        }
    }

    @Operation(
            summary = "Genera reporte de asistencia",
            description = "Devuelve una lista plana de asistencias con base en los filtros enviados.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    @GetMapping("/asistencia")
    public ResponseEntity<List<ReporteAsistenciaDto>> generarReporteAsistencia(@ModelAttribute ReporteFiltrosDto filtros) {
        aplicarFiltroDocenteAutenticado(filtros);
        return ResponseEntity.ok(reporteService.generarReporteAsistencias(filtros));
    }

    @Operation(
            summary = "Descarga reporte de asistencia en Excel",
            description = "Genera y descarga un archivo .xlsx con los datos de asistencia filtrados.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    @GetMapping("/asistencia/excel")
    public ResponseEntity<byte[]> descargarReporteExcel(@ModelAttribute ReporteFiltrosDto filtros) {
        aplicarFiltroDocenteAutenticado(filtros);
        List<ReporteAsistenciaDto> datos = reporteService.generarReporteAsistencias(filtros);
        byte[] excelBytes = excelExportService.generarReporteAsistencia(datos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_asistencias.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @Operation(
            summary = "Descarga reporte de asistencia en PDF",
            description = "Genera y descarga un archivo .pdf con los datos de asistencia filtrados.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    @GetMapping("/asistencia/pdf")
    public ResponseEntity<byte[]> descargarReportePdf(@ModelAttribute ReporteFiltrosDto filtros) {
        aplicarFiltroDocenteAutenticado(filtros);
        List<ReporteAsistenciaDto> datos = reporteService.generarReporteAsistencias(filtros);
        byte[] pdfBytes = pdfExportService.generarReporteAsistencia(datos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_asistencias.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
