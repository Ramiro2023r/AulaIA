package com.aulaia.controller;

import com.aulaia.dto.ApiErrorResponse;
import com.aulaia.dto.curso.CursoRequest;
import com.aulaia.dto.curso.CursoResponse;
import com.aulaia.service.CursoService;
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
 * Catálogo de cursos (Prompt 3.3).
 *
 * <p>Autorización: GET para ADMIN y DOCENTE; POST/PUT solo ADMIN
 * ({@code @PreAuthorize}, la política general la resuelve Spring Security).
 * Sin DELETE ni desactivación: el comportamiento funcional de la
 * eliminación de cursos no está definido de forma inequívoca en los
 * documentos (ver informe del Prompt 3.3), por lo que no se implementa
 * ninguna operación de ese tipo.
 */
@RestController
@RequestMapping("/api/v1/cursos")
@Tag(name = "Cursos", description = "Catálogo de cursos (módulo Cursos, Sprint 3)")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    @Operation(summary = "Listar cursos",
            description = "ADMIN y DOCENTE. Orden estable por id (creación).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de cursos",
                    content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<CursoResponse>> listar() {
        return ResponseEntity.ok(cursoService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener curso por id",
            description = "ADMIN y DOCENTE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Curso encontrado",
                    content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "COURSE_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<CursoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cursoService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear curso",
            description = "Solo ADMIN. No hay regla de unicidad de nombre "
                    + "(04-BD §6.6 no define UNIQUE): dos cursos pueden tener el mismo nombre.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Curso creado",
                    content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede crear cursos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CursoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cursoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar curso",
            description = "Solo ADMIN. 404 COURSE_NOT_FOUND si el curso no existe. "
                    + "Sin upsert: un id inexistente nunca crea un registro nuevo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Curso actualizado",
                    content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede modificar cursos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "COURSE_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<CursoResponse> actualizar(@PathVariable Long id,
                                                    @Valid @RequestBody CursoRequest request) {
        return ResponseEntity.ok(cursoService.actualizar(id, request));
    }
}