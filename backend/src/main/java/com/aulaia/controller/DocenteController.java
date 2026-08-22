package com.aulaia.controller;

import com.aulaia.dto.ApiErrorResponse;
import com.aulaia.dto.docente.DocenteProfileResponse;
import com.aulaia.dto.docente.DocenteProfileUpdateRequest;
import com.aulaia.dto.docente.DocenteRequest;
import com.aulaia.dto.docente.DocenteResponse;
import com.aulaia.dto.docente.DocenteUpdateRequest;
import com.aulaia.service.DocenteService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de docentes (Prompt 5.1, 07-PLAN).
 *
 * <p>Autorización: solo ADMIN puede administrar docentes (07-PLAN 5.1); el
 * rol DOCENTE no tiene endpoints documentados sobre este catálogo en 02-TRD,
 * por lo que todos los métodos están restringidos a ADMIN (incluidos los
 * GET). Sin DELETE: los documentos no lo definen; la desactivación es el
 * mecanismo documentado (06-FLUJOS #49). La contraseña nunca sale en
 * respuestas (solo entra write-only en el request de creación).
 */
@RestController
@RequestMapping("/api/v1/docentes")
@Tag(name = "Docentes", description = "Docentes y su cuenta DOCENTE (módulo Docentes, Sprint 5)")
public class DocenteController {

    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar docentes",
            description = "Solo ADMIN (07-PLAN 5.1: solo ADMIN administra docentes). "
                    + "Orden estable por id. Sin paginación (no documentada).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de docentes",
                    content = @Content(schema = @Schema(implementation = DocenteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede administrar docentes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<DocenteResponse>> listar() {
        return ResponseEntity.ok(docenteService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener docente por id",
            description = "Solo ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Docente encontrado",
                    content = @Content(schema = @Schema(implementation = DocenteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede administrar docentes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "TEACHER_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<DocenteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(docenteService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear docente",
            description = "Solo ADMIN (07-PLAN 5.1). Crea la cuenta DOCENTE y el perfil docente "
                    + "atómicamente (06-FLUJOS #9); la contraseña se almacena hasheada (BCrypt) y "
                    + "nunca se expone. 409 USERNAME_ALREADY_EXISTS si el username ya está en uso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Docente creado",
                    content = @Content(schema = @Schema(implementation = DocenteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede administrar docentes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "USERNAME_ALREADY_EXISTS",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<DocenteResponse> crear(@Valid @RequestBody DocenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(docenteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar docente",
            description = "Solo ADMIN. Actualiza únicamente nombres y apellidos; no cambia "
                    + "usuario, rol ni contraseña. 404 TEACHER_NOT_FOUND si el docente no existe.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Docente actualizado",
                    content = @Content(schema = @Schema(implementation = DocenteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (Bean Validation / JSON malformado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede administrar docentes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "TEACHER_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<DocenteResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody DocenteUpdateRequest request) {
        return ResponseEntity.ok(docenteService.actualizar(id, request));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar docente",
            description = "Solo ADMIN (06-FLUJOS #49). Pone activo = false en el docente y en su "
                    + "usuario (un usuario inactivo no puede iniciar sesión, 04-BD §6.1). Sin "
                    + "borrado físico: se mantienen históricos. Idempotente: desactivar un docente "
                    + "ya inactivo no produce error.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Docente desactivado",
                    content = @Content(schema = @Schema(implementation = DocenteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede administrar docentes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "TEACHER_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<DocenteResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(docenteService.desactivar(id));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Restablecer contraseña del docente",
            description = "Solo ADMIN. Establece una nueva contraseña para el docente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede restablecer contraseñas"),
            @ApiResponse(responseCode = "404", description = "TEACHER_NOT_FOUND")
    })
    public ResponseEntity<Void> restablecerPassword(@PathVariable Long id,
                                                    @Valid @RequestBody com.aulaia.dto.docente.ResetPasswordRequest request) {
        docenteService.restablecerPassword(id, request.password());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/perfil")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Obtener perfil del docente logueado",
            description = "Solo DOCENTE. Obtiene los datos del perfil actual.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil obtenido",
                    content = @Content(schema = @Schema(implementation = DocenteProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "TEACHER_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<DocenteProfileResponse> obtenerPerfil(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(docenteService.obtenerPerfilDocente(userDetails.getUsername()));
    }

    @PutMapping("/perfil")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Actualizar perfil del docente logueado",
            description = "Solo DOCENTE. Actualiza correo alternativo, teléfono y biografía.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil actualizado",
                    content = @Content(schema = @Schema(implementation = DocenteProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "TEACHER_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<DocenteProfileResponse> actualizarPerfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DocenteProfileUpdateRequest request) {
        return ResponseEntity.ok(docenteService.actualizarPerfilDocente(userDetails.getUsername(), request));
    }
}