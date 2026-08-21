package com.aulaia.controller;

import com.aulaia.dto.justificacion.EvaluarJustificacionRequest;
import com.aulaia.dto.justificacion.JustificacionRequest;
import com.aulaia.dto.justificacion.JustificacionResponse;
import com.aulaia.service.JustificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/justificaciones")
@RequiredArgsConstructor
@Tag(name = "Justificaciones", description = "API para gestionar justificaciones de inasistencia")
public class JustificacionController {

    private final JustificacionService justificacionService;

    @Operation(summary = "Crear nueva justificación", description = "Permite a un DOCENTE o ADMIN crear una solicitud de justificación")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public JustificacionResponse crear(
            @Valid @RequestBody JustificacionRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        return justificacionService.crear(request, userDetails.getUsername());
    }

    @Operation(summary = "Evaluar justificación", description = "Permite a un DOCENTE o ADMIN aprobar o rechazar una justificación")
    @PutMapping("/{id}/evaluar")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public JustificacionResponse evaluar(
            @PathVariable Long id,
            @Valid @RequestBody EvaluarJustificacionRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        return justificacionService.evaluar(id, request, userDetails.getUsername());
    }

    @Operation(summary = "Listar todas las justificaciones", description = "Devuelve el historial completo de justificaciones")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public List<JustificacionResponse> listarTodas() {
        return justificacionService.listarTodas();
    }

    @Operation(summary = "Obtener justificación por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public JustificacionResponse obtenerPorId(@PathVariable Long id) {
        return justificacionService.obtenerPorId(id);
    }
}
