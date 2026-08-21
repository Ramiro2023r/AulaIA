package com.aulaia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend AulaIA.
 *
 * API REST y reglas de negocio del Sistema Inteligente de Asistencia Escolar.
 * Ver documentación oficial en /docs (fuente de verdad del proyecto).
 */
@SpringBootApplication
public class AulaiaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AulaiaBackendApplication.class, args);
    }
}