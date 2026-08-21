package com.aulaia.controller;

import com.aulaia.dto.ApiErrorResponse;
import com.aulaia.dto.sesion.SesionClaseResponse;
import com.aulaia.entity.SesionClaseEstado;
import com.aulaia.service.SesionClaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API de sesiones de clase (Prompts 6.3 y 6.4, 07-PLAN).
 *
 * <p>Endpoints de este sprint: POST /api/v1/sesiones/{id}/abrir (6.3) y
 * GET /api/v1/sesiones, GET /api/v1/sesiones/activas,
 * GET /api/v1/sesiones/{id} (6.4, 03-ARQUITECTURA #25). Sin cierre,
 * cancelación ni creación por API. El Controller solo delega: no calcula
 * la hora, no resuelve ownership y no contiene lógica de estados.
 */
@RestController
@RequestMapping("/api/v1/sesiones")
@Tag(name = "Sesiones", description = "Sesiones de clase (Sprint 6)")
public class SesionClaseController {

    private final SesionClaseService sesionClaseService;

    public SesionClaseController(SesionClaseService sesionClaseService) {
        this.sesionClaseService = sesionClaseService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
    @Operation(summary = "Listar sesiones",
            description = "Filtros opcionales combinables (AND): fecha (YYYY-MM-DD), docente, "
                    + "seccion, curso y estado (PROGRAMADA/ABIERTA/CERRADA/CANCELADA). Sin "
                    + "paginación ni orden configurable (no documentados). ADMIN: todas; "
                    + "DOCENTE: solo las de sus horarios; el backend fuerza su identidad desde "
                    + "el JWT e ignora un docenteId ajeno (07-PLAN 6.4).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de sesiones",
                    content = @Content(schema = @Schema(implementation = SesionClaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (fecha o estado inválidos)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<SesionClaseResponse>> listar(
            @Parameter(description = "Filtrar por fecha (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Filtrar por id de docente (ignorado para DOCENTE)")
            @RequestParam(required = false) Long docente,
            @Parameter(description = "Filtrar por id de sección")
            @RequestParam(required = false) Long seccion,
            @Parameter(description = "Filtrar por id de curso")
            @RequestParam(required = false) Long curso,
            @Parameter(description = "Filtrar por estado (PROGRAMADA/ABIERTA/CERRADA/CANCELADA)")
            @RequestParam(required = false) SesionClaseEstado estado) {
        return ResponseEntity.ok(sesionClaseService.listarSesiones(fecha, docente, seccion, curso, estado));
    }

    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
    @Operation(summary = "Listar sesiones activas",
            description = "Sesiones en estado ABIERTA (única acepción documentada de 'sesión "
                    + "activa', 02-TRD §11). Sin filtros de query params (no documentados). "
                    + "ADMIN: todas; DOCENTE: solo las de sus horarios.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de sesiones activas",
                    content = @Content(schema = @Schema(implementation = SesionClaseResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<SesionClaseResponse>> listarActivas() {
        return ResponseEntity.ok(sesionClaseService.listarSesionesActivas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
    @Operation(summary = "Obtener sesión por id",
            description = "ADMIN: cualquier sesión. DOCENTE: solo si el horario le pertenece "
                    + "(403 si es de otro docente, sin revelar información).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión encontrada",
                    content = @Content(schema = @Schema(implementation = SesionClaseResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "DOCENTE consultando sesión ajena",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "SESSION_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SesionClaseResponse> buscarPorId(
            @Parameter(description = "Id de la sesión de clase") @PathVariable Long id) {
        return ResponseEntity.ok(sesionClaseService.buscarSesionPorId(id));
    }

    @PostMapping("/{id}/abrir")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
    @Operation(summary = "Abrir sesión",
            description = "ADMIN: cualquier sesión. DOCENTE: solo las de su propio horario (403 si "
                    + "es de otro docente). Solo PROGRAMADA pasa a ABIERTA con horaApertura = hora "
                    + "del servidor; una sesión ya ABIERTA es idempotente (conserva su horaApertura); "
                    + "CERRADA/CANCELADA se rechazan (409 SESSION_INVALID_STATE).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión abierta o ya estaba abierta",
                    content = @Content(schema = @Schema(implementation = SesionClaseResponse.class))),
            @ApiResponse(responseCode = "403", description = "FORBIDDEN (ajeno al docente)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "SESSION_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "SESSION_INVALID_STATE (CERRADA/CANCELADA)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SesionClaseResponse> abrir(
            @Parameter(description = "Id de la sesión de clase") @PathVariable Long id) {
        return ResponseEntity.ok(sesionClaseService.abrirSesion(id));
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
    @Operation(summary = "Cerrar sesión",
            description = "ADMIN: cualquier sesión. DOCENTE: solo las de su propio horario. "
                    + "Solo ABIERTA pasa a CERRADA con horaCierre = hora del servidor, generando inasistencias automáticas. "
                    + "Una sesión ya CERRADA es idempotente (conserva su horaCierre). "
                    + "PROGRAMADA/CANCELADA se rechazan (409 SESSION_INVALID_STATE).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión cerrada o ya estaba cerrada",
                    content = @Content(schema = @Schema(implementation = SesionClaseResponse.class))),
            @ApiResponse(responseCode = "403", description = "FORBIDDEN (ajeno al docente)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "SESSION_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "SESSION_INVALID_STATE (PROGRAMADA/CANCELADA)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SesionClaseResponse> cerrar(
            @Parameter(description = "Id de la sesión de clase") @PathVariable Long id) {
        return ResponseEntity.ok(sesionClaseService.cerrarSesion(id));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
    @Operation(summary = "Cancelar sesión",
            description = "ADMIN o DOCENTE cancelan una sesión. No puede estar en estado CERRADA.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión cancelada",
                    content = @Content(schema = @Schema(implementation = SesionClaseResponse.class))),
            @ApiResponse(responseCode = "403", description = "FORBIDDEN (ajeno al docente)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "SESSION_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "SESSION_INVALID_STATE (CERRADA)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SesionClaseResponse> cancelar(
            @Parameter(description = "Id de la sesión de clase") @PathVariable Long id) {
        return ResponseEntity.ok(sesionClaseService.cancelarSesion(id));
    }
}