package com.aulaia.exception;

/**
 * Excepción base: recurso solicitado no existe. HTTP 404.
 * Permite asociar un código funcional (p. ej. STUDENT_NOT_FOUND en el futuro).
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String code;

    public ResourceNotFoundException(String message) {
        this(message, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}