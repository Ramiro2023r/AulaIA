package com.aulaia.dto.asistencia;

import com.aulaia.entity.EstadoAsistencia;

import java.time.OffsetDateTime;

/**
 * Response del endpoint de registro de asistencia (Prompt 7.4 — 07-PLAN Sprint 7).
 *
 * <p>Campos documentados en 07-PLAN 7.4:
 * <ul>
 *   <li>{@code success} — {@code true} si el registro fue exitoso.</li>
 *   <li>{@code nombre}  — nombre del estudiante (solo {@code nombres},
 *       sin apellidos: "No devolver apellidos ni información innecesaria al
 *       Modo Aula", 07-PLAN 7.4).</li>
 *   <li>{@code hora}    — momento exacto de registro (hora del servidor).</li>
 *   <li>{@code estado}  — {@code PRESENTE} o {@code TARDANZA}.</li>
 *   <li>{@code mensaje} — texto legible para mostrar en la pantalla del Modo Aula.</li>
 * </ul>
 */
public record RegistrarAsistenciaResponse(

        boolean success,
        String nombre,
        OffsetDateTime hora,
        EstadoAsistencia estado,
        String mensaje
) {}
