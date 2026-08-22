package com.aulaia.service;

import com.aulaia.client.telegram.TelegramBotClient;
import com.aulaia.entity.Apoderado;
import com.aulaia.entity.Estudiante;
import com.aulaia.repository.EstudianteApoderadoRepository;
import com.aulaia.repository.EstudianteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/** Envía avisos de asistencia solamente después del commit de una asistencia nueva. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true", matchIfMissing = false)
public class TelegramAsistenciaNotificationListener {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final EstudianteApoderadoRepository estudianteApoderadoRepository;
    private final TelegramBotClient telegramBotClient;
    private final ObjectProvider<EstudianteRepository> estudianteRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void notificarAsistencia(AsistenciaRegistradaEvent event) {
        Set<String> chatIdsNotificados = new HashSet<>();
        String nombreEstudiante = resolverNombreEstudiante(event);

        try {
            estudianteApoderadoRepository.findWithApoderadoByEstudianteId(event.estudianteId()).forEach(relacion -> {
                Apoderado apoderado = relacion.getApoderado();
                if (!esApoderadoVinculadoActivo(apoderado)) {
                    return;
                }

                String chatId = apoderado.getTelegramChatId().trim();
                if (!chatIdsNotificados.add(chatId)) {
                    return;
                }

                try {
                    telegramBotClient.sendMessage(Long.parseLong(chatId), crearMensaje(event, nombreEstudiante));
                } catch (NumberFormatException ex) {
                    log.warn("Se omitió una notificación Telegram por un destino no válido. Detalle oculto por seguridad.");
                } catch (RuntimeException ex) {
                    // Un destinatario fallido no interrumpe los siguientes ni afecta la asistencia ya confirmada.
                    log.warn("No fue posible enviar una notificación de asistencia por Telegram. Detalle oculto por seguridad.");
                    log.debug("Detalle del error de notificación Telegram: {}", ex.getClass().getName());
                }
            });
        } catch (RuntimeException ex) {
            // El evento llega después del commit: esta falla nunca revierte la asistencia.
            log.warn("No fue posible preparar las notificaciones de asistencia por Telegram. Detalle oculto por seguridad.");
            log.debug("Detalle de preparación de notificaciones Telegram: {}", ex.getClass().getName());
        }
    }

    static String crearMensaje(AsistenciaRegistradaEvent event) {
        return crearMensaje(event, normalizarNombre(event.nombreEstudiante()));
    }

    private static String crearMensaje(AsistenciaRegistradaEvent event, String nombreEstudiante) {
        StringBuilder mensaje = new StringBuilder("🎓 AulaIA - Asistencia\n\n")
                .append("✅ ").append(nombreEstudiante).append(" registró su asistencia.\n\n")
                .append("📅 Fecha: ").append(FECHA.format(event.fechaHora())).append("\n")
                .append("🕐 Hora: ").append(HORA.format(event.fechaHora())).append("\n");

        if (event.curso() != null && !event.curso().isBlank()) {
            mensaje.append("📚 Curso: ").append(event.curso().trim()).append("\n");
        }
        if (event.gradoSeccion() != null && !event.gradoSeccion().isBlank()) {
            mensaje.append("🏫 Grado/Sección: ").append(event.gradoSeccion().trim()).append("\n");
        }
        return mensaje.append("\nEstado: ").append(event.estado()).toString();
    }

    private boolean esApoderadoVinculadoActivo(Apoderado apoderado) {
        return apoderado != null
                && apoderado.isActivo()
                && apoderado.getTelegramChatId() != null
                && !apoderado.getTelegramChatId().isBlank()
                && apoderado.getTelegramVinculadoAt() != null;
    }

    private String resolverNombreEstudiante(AsistenciaRegistradaEvent event) {
        String nombreDelEvento = normalizarNombre(event.nombreEstudiante());
        if (!"El estudiante".equals(nombreDelEvento)) {
            return nombreDelEvento;
        }

        EstudianteRepository repository = estudianteRepository.getIfAvailable();
        if (repository == null || event.estudianteId() == null) {
            return nombreDelEvento;
        }

        return repository.findById(event.estudianteId())
                .map(this::nombreCompleto)
                .map(TelegramAsistenciaNotificationListener::normalizarNombre)
                .orElse(nombreDelEvento);
    }

    private String nombreCompleto(Estudiante estudiante) {
        String nombres = estudiante.getNombres() == null ? "" : estudiante.getNombres().trim();
        String apellidos = estudiante.getApellidos() == null ? "" : estudiante.getApellidos().trim();
        return (nombres + " " + apellidos).trim();
    }

    private static String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "El estudiante";
        }
        String normalizado = nombre.trim();
        if (normalizado.equalsIgnoreCase("undefined")
                || normalizado.equalsIgnoreCase("null")
                || normalizado.toLowerCase().contains("undefined")) {
            return "El estudiante";
        }
        return normalizado;
    }
}
