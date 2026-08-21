package com.aulaia.entity;

/**
 * Estados posibles de una sesión de clase (04-BD §7.1, 07-PLAN 6.1).
 *
 * <p>Valores EXACTOS documentados: PROGRAMADA, ABIERTA, CERRADA,
 * CANCELADA. No se agregan estados adicionales. Las transiciones de
 * estado pertenecen a prompts posteriores (6.3+); aquí solo se modelan
 * los estados válidos.
 */
public enum SesionClaseEstado {

    PROGRAMADA,
    ABIERTA,
    CERRADA,
    CANCELADA
}