package com.aulaia.entity;

/**
 * Estado de una asistencia (tabla {@code asistencias}, 04-BD §8.1).
 *
 * <p>Valores exactos según 04-BD §8.1 y 07-PLAN §8:
 * <ul>
 *   <li>{@link #PRESENTE} — asistió dentro de la tolerancia.</li>
 *   <li>{@link #TARDANZA} — asistió pero fuera de la tolerancia.</li>
 *   <li>{@link #AUSENTE}  — no asistió (generado al cerrar sesión).</li>
 *   <li>{@link #JUSTIFICADO} — ausencia justificada posteriormente.</li>
 * </ul>
 *
 * <p>Mapeado con {@code @Enumerated(EnumType.STRING)} — nunca ordinal.
 * El cálculo de PRESENTE/TARDANZA corresponde al Prompt 7.3.
 * La generación de AUSENTE corresponde al cierre de sesión (prompt posterior).
 * La transición a JUSTIFICADO corresponde al Sprint 14.
 */
public enum EstadoAsistencia {
    PRESENTE,
    TARDANZA,
    AUSENTE,
    JUSTIFICADO
}
