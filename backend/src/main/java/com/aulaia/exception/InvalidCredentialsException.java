package com.aulaia.exception;

/**
 * Credenciales inválidas en login. HTTP 401, código INVALID_CREDENTIALS.
 *
 * <p>Respuesta externa idéntica para: username inexistente, password
 * incorrecta y usuario inactivo (se evita la enumeración de usuarios).
 */
public class InvalidCredentialsException extends RuntimeException {

    public static final String MESSAGE = "Usuario o contraseña incorrectos";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }
}