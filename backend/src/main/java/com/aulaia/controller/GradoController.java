package com.aulaia.controller;

import com.aulaia.dto.ApiErrorResponse;
import com.aulaia.dto.grado.GradoRequest;
import com.aulaia.dto.grado.GradoResponse;
import com.aulaia.service.GradoService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo de grados escolares (Prompt 3.1).
 *
 * <p>Autorización: GET para ADMIN y DOCENTE; POST/PUT solo ADMIN
 * ({@code @PreAuthorize}, la política general la resuelve Spring Security).
 */
@RestController
@RequestMapping("/api/v1/grados")
@Tag(name = "Grados", description = "Catálogo de grados escolares (módulo Grados, Sprint 3)")
public class GradoController {

    private final GradoService gradoService;

    public GradoController(GradoService gradoService) {
        this.gradoService = gradoService;
    }

    @GetMapping
    @Operation(summary = "Listar grados",
            description = "ADMIN y DOCENTE. Orden estable por id (creación).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de grados"),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<GradoResponse>> listar() {
        return ResponseEntity.ok(gradoService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener grado por id",
            description = "ADMIN y DOCENTE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grado encontrado",
                    content = @Content(schema = @Schema(implementation = GradoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "GRADE_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<GradoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(gradoService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear grado",
            description = "Solo ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Grado creado",
                    content = @Content(schema = @Schema(implementation = GradoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede crear grados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<GradoResponse> crear(@Valid @RequestBody GradoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gradoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar grado",
            description = "Solo ADMIN. 404 GRADE_NOT_FOUND si no existe.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grado actualizado",
                    content = @Content(schema = @Schema(implementation = GradoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede modificar grados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "GRADE_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<GradoResponse> actualizar(@PathVariable Long id,
                                                    @Valid @RequestBody GradoRequest request) {
        return ResponseEntity.ok(gradoService.actualizar(id, request));
    }
}