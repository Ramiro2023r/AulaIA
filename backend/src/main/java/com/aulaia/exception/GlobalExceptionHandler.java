package com.aulaia.exception;

import com.aulaia.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global de errores de la API REST de AulaIA.
 *
 * Errores esperados de negocio: WARN.
 * Errores inesperados: ERROR, sin exponer detalles internos al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String VALIDATION_MESSAGE = "Los datos enviados no son válidos";
    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    private static final String INTERNAL_MESSAGE = "Ocurrió un error interno";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildBusinessError(ex, HttpStatus.NOT_FOUND, ex.getCode(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        return buildBusinessError(ex, HttpStatus.BAD_REQUEST, ex.getCode(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ConflictException ex, HttpServletRequest request) {
        return buildBusinessError(ex, HttpStatus.CONFLICT, ex.getCode(), request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(
            ForbiddenOperationException ex, HttpServletRequest request) {
        return buildBusinessError(ex, HttpStatus.FORBIDDEN, ex.getCode(), request);
    }

    /**
     * Credenciales inválidas en login: 401 con respuesta idéntica para
     * username inexistente, password incorrecta e inactivo (sin
     * enumeración de usuarios).
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("Credenciales inválidas en {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "INVALID_CREDENTIALS",
                        ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
        log.warn("Validación fallida en {}: {}", request.getRequestURI(), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), VALIDATION_ERROR,
                        VALIDATION_MESSAGE, request.getRequestURI(), details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(violation -> details.put(
                        violation.getPropertyPath().toString(), violation.getMessage()));
        log.warn("Validación fallida en {}: {}", request.getRequestURI(), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), VALIDATION_ERROR,
                        VALIDATION_MESSAGE, request.getRequestURI(), details));
    }

    /**
     * Parámetros de query con tipo o formato inválido (Prompt 6.4: fecha
     * que no es {@code YYYY-MM-DD} o estado fuera de los 4 oficiales) →
     * 400 VALIDATION_ERROR, nunca 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Parámetro inválido en {}: {} = {}",
                request.getRequestURI(), ex.getName(), ex.getValue());
        Map<String, String> details = new LinkedHashMap<>();
        details.put(ex.getName(), "Valor no válido: " + ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), VALIDATION_ERROR,
                        VALIDATION_MESSAGE, request.getRequestURI(), details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("JSON inválido en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "INVALID_JSON",
                        "El cuerpo de la solicitud no es válido", request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado: {} {}", ex.getHttpMethod(), ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiErrorResponse.of(HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND",
                        "Recurso no encontrado", request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Método no soportado: {} {}", ex.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                ApiErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED.value(), "METHOD_NOT_ALLOWED",
                        "El método HTTP no está soportado para este endpoint", request.getRequestURI()));
    }

    /**
     * Denegaciones a nivel de método (@PreAuthorize / roles), que ocurren
     * dentro del DispatcherServlet: 401 si el usuario es anónimo, 403 si
     * está autenticado pero sin permisos. Las denegaciones a nivel de
     * filtro web ya las manejan {@code RestAuthenticationEntryPoint} y
     * {@code RestAccessDeniedHandler}.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
        if (anonymous) {
            log.warn("Autenticación requerida en {}", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                            "Autenticación requerida", request.getRequestURI()));
        }
        log.warn("Acceso denegado en {} para {}: {}",
                request.getRequestURI(), authentication.getName(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiErrorResponse.of(HttpStatus.FORBIDDEN.value(), "FORBIDDEN",
                        "No tienes permiso para realizar esta acción", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), INTERNAL_ERROR,
                        INTERNAL_MESSAGE, request.getRequestURI()));
    }

    private ResponseEntity<ApiErrorResponse> buildBusinessError(
            RuntimeException ex, HttpStatus status, String code, HttpServletRequest request) {
        log.warn("Error de negocio {} {} en {}: {}",
                status.value(), code, request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(status).body(
                ApiErrorResponse.of(status.value(), code, ex.getMessage(), request.getRequestURI()));
    }
}