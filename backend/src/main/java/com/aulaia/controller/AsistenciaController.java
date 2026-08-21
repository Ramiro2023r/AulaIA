package com.aulaia.controller;

import com.aulaia.dto.asistencia.AsistenciaCorreccionRequest;
import com.aulaia.dto.asistencia.AsistenciaResponse;
import com.aulaia.dto.asistencia.RegistrarAsistenciaRequest;
import com.aulaia.dto.asistencia.RegistrarAsistenciaResponse;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.service.AsistenciaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controlador REST para el registro de asistencias (Prompt 7.4).
 *
 * <p>El Modo Aula es operado por el docente, por lo que requiere rol
 * DOCENTE o ADMIN. Las validaciones funcionales (sesión inactiva, estudiante
 * no hallado, etc.) son capturadas por el manejador global de excepciones
 * implementado en el Sprint 1 y convertidas a códigos de error estandarizados.
 */
@RestController
@RequestMapping("/api/v1/asistencias")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    /**
     * Endpoint para registrar asistencia mediante QR o código manual.
     * Usado principalmente desde el Modo Aula.
     *
     * @param request Datos del registro (código, método y ID de sesión).
     * @return 200 OK con el resultado (nombre del estudiante, estado y hora).
     */
    @PostMapping("/registrar")
    @PreAuthorize("hasRole('DOCENTE') or hasRole('ADMIN')")
    public ResponseEntity<RegistrarAsistenciaResponse> registrar(
            @Valid @RequestBody RegistrarAsistenciaRequest request) {
        
        RegistrarAsistenciaResponse response = asistenciaService.registrar(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/correccion")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
    @Operation(summary = "Corrección manual de asistencia",
            description = "DOCENTE o ADMIN actualizan manualmente la asistencia y adjuntan un motivo (14.2).")
    public ResponseEntity<AsistenciaResponse> correccionManual(
            @PathVariable Long id,
            @Valid @RequestBody AsistenciaCorreccionRequest request) {
        return ResponseEntity.ok(asistenciaService.correccionManual(id, request));
    }

    /**
     * GET /api/v1/asistencias
     */
    @GetMapping
    @PreAuthorize("hasRole('DOCENTE') or hasRole('ADMIN')")
    public ResponseEntity<Page<AsistenciaResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoAsistencia estado,
            @RequestParam(required = false) Long seccion,
            @RequestParam(required = false) Long curso,
            @RequestParam(required = false) Long estudiante,
            Pageable pageable) {

        return ResponseEntity.ok(asistenciaService.listar(fecha, estado, seccion, curso, estudiante, pageable));
    }

    /**
     * GET /api/v1/asistencias/sesion/{id}
     */
    @GetMapping("/sesion/{id}")
    @PreAuthorize("hasRole('DOCENTE') or hasRole('ADMIN')")
    public ResponseEntity<Page<AsistenciaResponse>> listarPorSesion(
            @PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(asistenciaService.listarPorSesion(id, pageable));
    }

    /**
     * GET /api/v1/asistencias/estudiante/{id}
     */
    @GetMapping("/estudiante/{id}")
    @PreAuthorize("hasRole('DOCENTE') or hasRole('ADMIN')")
    public ResponseEntity<Page<AsistenciaResponse>> listarPorEstudiante(
            @PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(asistenciaService.listarPorEstudiante(id, pageable));
    }
}
