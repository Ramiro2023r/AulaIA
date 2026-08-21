package com.aulaia.entity;

/**
 * Método de registro de una asistencia (tabla {@code asistencias}, 04-BD §8.1).
 *
 * <p>Valores exactos según 04-BD §8.1 y 07-PLAN §9:
 * <ul>
 *   <li>{@link #QR}            — lectura por cámara del código QR del estudiante.</li>
 *   <li>{@link #CODIGO}        — ingreso manual del código escolar por el estudiante.</li>
 *   <li>{@link #MANUAL_DOCENTE}— ingreso manual realizado por el docente.</li>
 *   <li>{@link #SISTEMA}       — generado automáticamente por el sistema (ej. ausentes).</li>
 * </ul>
 *
 * <p>CONFLICTO DOCUMENTAL detectado y reportado en Prompt 7.1:
 * El 02-TRD no define explícitamente los valores del enum en esta versión
 * revisada del documento. Los valores de 04-BD + 07-PLAN son coincidentes
 * y se usan como fuente canónica: QR, CODIGO, MANUAL_DOCENTE, SISTEMA.
 *
 * <p>Mapeado con {@code @Enumerated(EnumType.STRING)} — nunca ordinal.
 */
public enum MetodoRegistro {
    QR,
    CODIGO,
    MANUAL_DOCENTE,
    SISTEMA
}
