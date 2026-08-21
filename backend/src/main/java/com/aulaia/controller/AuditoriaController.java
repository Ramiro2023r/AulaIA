package com.aulaia.controller;

import com.aulaia.dto.auditoria.AuditoriaResponse;
import com.aulaia.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "Consulta de registros de auditoría — solo ADMIN")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @Operation(summary = "Listar registros de auditoría con filtros opcionales")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditoriaResponse> listar(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return auditoriaService.buscarConFiltros(usuario, entidad, accion, desde, hasta);
    }
}
