package com.aulaia.exception;

/**
 * Excepción base: violación de una regla de negocio. HTTP 400.
 * Permite asociar un código funcional.
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String message) {
        this(message, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}