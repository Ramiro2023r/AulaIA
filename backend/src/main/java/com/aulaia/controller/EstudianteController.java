package com.aulaia.controller;

import com.aulaia.dto.ApiErrorResponse;
import com.aulaia.dto.estudiante.EstudianteRequest;
import com.aulaia.dto.estudiante.EstudianteResponse;
import com.aulaia.dto.estudiante.RegenerarQrResponse;
import com.aulaia.service.EstudianteService;
import com.aulaia.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de estudiantes (Prompt 4.3, 07-PLAN).
 *
 * <p>Autorización: GET para ADMIN y DOCENTE (las restricciones de contexto
 * por sección para DOCENTE se aplicarán en un prompt posterior); POST/PUT/
 * PATCH solo ADMIN ({@code @PreAuthorize}). Sin DELETE: el plan no lo
 * define; la desactivación es el mecanismo documentado (PATCH desactivar,
 * 06-FLUJOS #48). El qrToken no entra desde el frontend (lo genera el
 * Service) ni se expone en respuestas.
 */
@RestController
@RequestMapping("/api/v1/estudiantes")
@Tag(name = "Estudiantes", description = "Estudiantes y su sección (módulo Estudiantes, Sprint 4)")
public class EstudianteController {

    private final EstudianteService estudianteService;
    private final QrCodeService qrCodeService;

    public EstudianteController(EstudianteService estudianteService, QrCodeService qrCodeService) {
        this.estudianteService = estudianteService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping
    @Operation(summary = "Listar estudiantes",
            description = "ADMIN y DOCENTE. Filtros combinables: codigo (igualdad exacta), "
                    + "nombre (coincidencia parcial sobre nombres), seccion (seccionId) y activo. "
                    + "Orden estable por id. Sin paginación.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de estudiantes",
                    content = @Content(schema = @Schema(implementation = EstudianteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<EstudianteResponse>> listar(
            @Parameter(description = "Filtro por codigo (igualdad exacta)")
            @RequestParam(required = false) String codigo,
            @Parameter(description = "Filtro por nombre (coincidencia parcial sobre nombres)")
            @RequestParam(required = false) String nombre,
            @Parameter(description = "Filtro por id de seccion")
            @RequestParam(required = false) Long seccion,
            @Parameter(description = "Filtro por estado activo")
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(estudianteService.listar(codigo, nombre, seccion, activo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener estudiante por id",
            description = "ADMIN y DOCENTE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudiante encontrado",
                    content = @Content(schema = @Schema(implementation = EstudianteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "STUDENT_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<EstudianteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear estudiante",
            description = "Solo ADMIN. El qrToken lo genera el Service de forma segura; "
                    + "404 SECTION_NOT_FOUND si la sección no existe; 409 STUDENT_CODE_ALREADY_EXISTS "
                    + "si el codigo ya está en uso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Estudiante creado",
                    content = @Content(schema = @Schema(implementation = EstudianteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede crear estudiantes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "SECTION_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "STUDENT_CODE_ALREADY_EXISTS",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<EstudianteResponse> crear(@Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estudianteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar estudiante",
            description = "Solo ADMIN. No regenera el qrToken. "
                    + "404 STUDENT_NOT_FOUND si el estudiante no existe; 404 SECTION_NOT_FOUND si la "
                    + "sección no existe; 409 STUDENT_CODE_ALREADY_EXISTS si el codigo ya pertenece a "
                    + "otro estudiante.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudiante actualizado",
                    content = @Content(schema = @Schema(implementation = EstudianteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede modificar estudiantes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "STUDENT_NOT_FOUND / SECTION_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "STUDENT_CODE_ALREADY_EXISTS",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<EstudianteResponse> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.ok(estudianteService.actualizar(id, request));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar estudiante",
            description = "Solo ADMIN. activo = false sin borrado físico (06-FLUJOS #48); "
                    + "conserva codigo, qrToken, nombres, apellidos y sección. Idempotente: "
                    + "desactivar un estudiante ya inactivo no produce error.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudiante desactivado",
                    content = @Content(schema = @Schema(implementation = EstudianteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede desactivar estudiantes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "STUDENT_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<EstudianteResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.desactivar(id));
    }

    @PostMapping("/{id}/regenerar-qr")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Regenerar token QR",
            description = "Solo ADMIN (Prompt 4.4, 07-PLAN). Genera un nuevo qrToken opaco y "
                    + "único; el anterior queda inválido al dejar de persistirse. El token no se "
                    + "expone (privacidad: 04-BD §22); el refresco visual del QR corresponde al "
                    + "endpoint de imagen (Prompt 4.5). No se genera imagen QR aquí.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "QR regenerado",
                    content = @Content(schema = @Schema(implementation = RegenerarQrResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede regenerar el QR",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "STUDENT_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<RegenerarQrResponse> regenerarQr(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.regenerarQrToken(id));
    }

    @GetMapping("/{id}/qr")
    @Operation(summary = "Obtener imagen QR del estudiante",
            description = "ADMIN y DOCENTE (02-TRD: obtener QR autorizado). Genera bajo demanda el "
                    + "PNG del QR con contenido exacto AULAIA:STUDENT:<qrToken> (07-PLAN 4.5, "
                    + "06-FLUJOS #7); sin datos personales (04-BD §22), sin regenerar el token. "
                    + "La imagen no se persiste ni se cachea.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Imagen QR en PNG",
                    content = @Content(mediaType = "image/png")),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "STUDENT_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<byte[]> obtenerQr(@PathVariable Long id) {
        String contenido = estudianteService.contenidoQr(id);
        byte[] png = qrCodeService.generarPng(contenido);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}