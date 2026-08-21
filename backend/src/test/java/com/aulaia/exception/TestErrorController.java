package com.aulaia.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador SOLO de prueba (vive en src/test, nunca llega al jar de
 * producción). Permite verificar GlobalExceptionHandler con MockMvc sin
 * exponer endpoints funcionales permanentes.
 */
@RestController
@RequestMapping("/test/error")
public class TestErrorController {

    @GetMapping("/resource-not-found")
    public void resourceNotFound() {
        throw new ResourceNotFoundException("Recurso no encontrado");
    }

    @GetMapping("/business")
    public void business() {
        throw new BusinessException("Regla de negocio violada");
    }

    @GetMapping("/conflict")
    public void conflict() {
        throw new ConflictException("Conflicto de datos");
    }

    @GetMapping("/forbidden")
    public void forbidden() {
        throw new ForbiddenOperationException("Operación no permitida");
    }

    @GetMapping("/unexpected")
    public void unexpected() {
        throw new IllegalStateException("Fallo inesperado de prueba");
    }

    @PostMapping("/validation")
    public void validation(@Valid @RequestBody TestPayload payload) {
    }

    @PostMapping("/payload")
    public void payload(@RequestBody TestPayload payload) {
    }

    public record TestPayload(@NotBlank String username, @NotBlank String nombre) {
    }
}