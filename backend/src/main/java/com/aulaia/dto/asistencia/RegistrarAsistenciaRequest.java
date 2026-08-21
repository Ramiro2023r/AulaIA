package com.aulaia.dto.asistencia;

import com.aulaia.entity.MetodoRegistro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request para registrar una asistencia desde el Modo Aula (Prompt 7.4).
 *
 * <p>Campos documentados en 07-PLAN 7.4:
 * <ul>
 *   <li>{@code codigo}  — contenido del QR ({@code AULAIA:STUDENT:<TOKEN>})
 *       o código escolar del estudiante.</li>
 *   <li>{@code metodo}  — cómo se identifica al estudiante ({@code QR} o
 *       {@code CODIGO}).</li>
 *   <li>{@code sesionId} — ID de la sesión de clase a la que se registra.</li>
 * </ul>
 *
 * <p>La hora de registro NUNCA proviene de este request (07-PLAN 7.3:
 * "Nunca confiar en la hora enviada por frontend"); el servicio usa
 * {@code Clock} del servidor.
 */
public record RegistrarAsistenciaRequest(

        @NotBlank(message = "codigo no puede estar vacío")
        String codigo,

        @NotNull(message = "metodo es obligatorio")
        MetodoRegistro metodo,

        @NotNull(message = "sesionId es obligatorio")
        Long sesionId
) {}
