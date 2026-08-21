package com.aulaia.controller;

import com.aulaia.dto.ApiErrorResponse;
import com.aulaia.dto.seccion.SeccionRequest;
import com.aulaia.dto.seccion.SeccionResponse;
import com.aulaia.service.SeccionService;
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
 * Catálogo de secciones académicas (Prompt 3.2).
 *
 * <p>Autorización: GET para ADMIN y DOCENTE; POST/PUT solo ADMIN
 * ({@code @PreAuthorize}, la política general la resuelve Spring Security).
 * Sin DELETE: el Prompt 3.2 no lo exige.
 */
@RestController
@RequestMapping("/api/v1/secciones")
@Tag(name = "Secciones", description = "Secciones académicas por grado y periodo (módulo Secciones, Sprint 3)")
public class SeccionController {

    private final SeccionService seccionService;

    public SeccionController(SeccionService seccionService) {
        this.seccionService = seccionService;
    }

    @GetMapping
    @Operation(summary = "Listar secciones",
            description = "ADMIN y DOCENTE. Orden estable por id (creación).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de secciones",
                    content = @Content(schema = @Schema(implementation = SeccionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<SeccionResponse>> listar() {
        return ResponseEntity.ok(seccionService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener sección por id",
            description = "ADMIN y DOCENTE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sección encontrada",
                    content = @Content(schema = @Schema(implementation = SeccionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "SECTION_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SeccionResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(seccionService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear sección",
            description = "Solo ADMIN. 404 GRADE_NOT_FOUND si el grado no existe; "
                    + "409 SECTION_ALREADY_EXISTS si ya existe una sección con el mismo grado, nombre y periodo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sección creada",
                    content = @Content(schema = @Schema(implementation = SeccionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede crear secciones",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "GRADE_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "SECTION_ALREADY_EXISTS",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SeccionResponse> crear(@Valid @RequestBody SeccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seccionService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar sección",
            description = "Solo ADMIN. 404 SECTION_NOT_FOUND si la sección no existe; "
                    + "404 GRADE_NOT_FOUND si el grado no existe; "
                    + "409 SECTION_ALREADY_EXISTS si otro grado+nombre+periodo idéntico la bloquea.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sección actualizada",
                    content = @Content(schema = @Schema(implementation = SeccionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede modificar secciones",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "SECTION_NOT_FOUND / GRADE_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "SECTION_ALREADY_EXISTS",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SeccionResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody SeccionRequest request) {
        return ResponseEntity.ok(seccionService.actualizar(id, request));
    }
}