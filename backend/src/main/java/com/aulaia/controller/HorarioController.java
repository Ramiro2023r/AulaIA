package com.aulaia.controller;

import com.aulaia.dto.ApiErrorResponse;
import com.aulaia.dto.horario.HorarioRequest;
import com.aulaia.dto.horario.HorarioResponse;
import com.aulaia.service.HorarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.util.List;

/**
 * API de horarios (Prompt 5.4, 07-PLAN).
 *
 * <p>Endpoints documentados (03-ARQUITECTURA #24: GET, POST, PUT; sin
 * DELETE: los documentos no lo definen para horarios). Solo ADMIN crea y
 * modifica (07-PLAN 5.4). DOCENTE consulta únicamente sus propios
 * horarios: la restricción se aplica en el Service derivando la identidad
 * desde la sesión/JWT, nunca desde parámetros del request (Prompt 5.4 §17).
 */
@RestController
@RequestMapping("/api/v1/horarios")
@Tag(name = "Horarios", description = "Horarios semanales de curso/sección/docente (Sprint 5)")
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
    @Operation(summary = "Listar horarios",
            description = "ADMIN: todos los horarios con filtros opcionales (docente, seccion, "
                    + "curso, dia), combinables. DOCENTE: solo sus propios horarios; el backend "
                    + "fuerza su identidad desde el JWT e ignora un docenteId ajeno (07-PLAN 5.4). "
                    + "Sin paginación (no documentada).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de horarios",
                    content = @Content(schema = @Schema(implementation = HorarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (dia fuera de 1-7)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<HorarioResponse>> listar(
            @Parameter(description = "Filtrar por id de docente (ignorado para DOCENTE)")
            @RequestParam(required = false) Long docente,
            @Parameter(description = "Filtrar por id de sección")
            @RequestParam(required = false) Long seccion,
            @Parameter(description = "Filtrar por id de curso")
            @RequestParam(required = false) Long curso,
            @Parameter(description = "Filtrar por día (1 = Lunes … 7 = Domingo)")
            @RequestParam(required = false) Short dia) {
        return ResponseEntity.ok(horarioService.listarHorarios(docente, seccion, curso, dia));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
    @Operation(summary = "Obtener horario por id",
            description = "ADMIN: cualquier horario. DOCENTE: solo si el horario le pertenece "
                    + "(403 si es de otro docente, sin revelar información).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Horario encontrado",
                    content = @Content(schema = @Schema(implementation = HorarioResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "DOCENTE consultando horario ajeno",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "SCHEDULE_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<HorarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(horarioService.buscarHorario(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear horario",
            description = "Solo ADMIN (07-PLAN 5.4). Valida relaciones existentes y conflictos "
                    + "de docente/sección (Prompt 5.3); horarios consecutivos son válidos. "
                    + "activo queda en TRUE (default 04-BD §6.7).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Horario creado",
                    content = @Content(schema = @Schema(implementation = HorarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede crear horarios",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "COURSE_NOT_FOUND / SECTION_NOT_FOUND / TEACHER_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "TEACHER_SCHEDULE_CONFLICT / SECTION_SCHEDULE_CONFLICT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<HorarioResponse> crear(@Valid @RequestBody HorarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(horarioService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar horario",
            description = "Solo ADMIN. Sin upsert: 404 SCHEDULE_NOT_FOUND si no existe. Los "
                    + "conflictos se validan excluyendo el propio id. activo no es administrable.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Horario actualizado",
                    content = @Content(schema = @Schema(implementation = HorarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede modificar horarios",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "SCHEDULE_NOT_FOUND / relaciones",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "TEACHER_SCHEDULE_CONFLICT / SECTION_SCHEDULE_CONFLICT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<HorarioResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody HorarioRequest request) {
        return ResponseEntity.ok(horarioService.actualizar(id, request));
    }
}