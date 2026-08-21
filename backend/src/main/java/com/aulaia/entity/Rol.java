package com.aulaia.entity;

/**
 * Roles de usuario del sistema (docs/04-BASE_DE_DATOS §6.1).
 *
 * Los estudiantes NO tienen cuenta: la asistencia en Modo Aula se
 * registra sin autenticación individual.
 */
public enum Rol {
    ADMIN,
    DOCENTE
}