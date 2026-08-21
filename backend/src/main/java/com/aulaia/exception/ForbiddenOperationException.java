package com.aulaia.exception;

/**
 * Excepción base: operación no permitida por permisos/rol. HTTP 403.
 * Preparada para los códigos UNAUTHORIZED/FORBIDDEN del Sprint 2.
 */
public class ForbiddenOperationException extends RuntimeException {

    private final String code;

    public ForbiddenOperationException(String message) {
        this(message, "FORBIDDEN");
    }

    public ForbiddenOperationException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}