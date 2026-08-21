package com.aulaia.controller;

import com.aulaia.dto.dashboard.DashboardDocenteResponse;
import com.aulaia.service.DashboardDocenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardDocenteController {

    private final DashboardDocenteService dashboardService;

    public DashboardDocenteController(DashboardDocenteService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/docente")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<DashboardDocenteResponse> getDashboardDocente() {
        return ResponseEntity.ok(dashboardService.obtenerResumen());
    }
}
