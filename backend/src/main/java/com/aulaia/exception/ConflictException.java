package com.aulaia.exception;

/**
 * Excepción base: conflicto de estado/datos (p. ej. duplicados). HTTP 409.
 * Permite asociar un código funcional (p. ej. ATTENDANCE_ALREADY_REGISTERED
 * en el futuro).
 */
public class ConflictException extends RuntimeException {

    private final String code;

    public ConflictException(String message) {
        this(message, "CONFLICT");
    }

    public ConflictException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}