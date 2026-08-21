package com.aulaia.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador SOLO de prueba (vive en src/test, nunca llega al jar de
 * producción). Verifica 401/403 y roles con MockMvc sin endpoints
 * productivos.
 */
@RestController
@RequestMapping("/test/security")
public class TestSecurityController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly() {
        return "ok";
    }

    @GetMapping("/docente")
    @PreAuthorize("hasRole('DOCENTE')")
    public String docenteOnly() {
        return "ok";
    }

    @GetMapping("/me")
    public String me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName() + "|" + authentication.getAuthorities();
    }
}