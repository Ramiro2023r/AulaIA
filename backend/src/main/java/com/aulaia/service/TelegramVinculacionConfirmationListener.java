package com.aulaia.service;

import com.aulaia.client.telegram.TelegramBotClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Envía la confirmación solo una vez que la transacción de vinculación hizo commit. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true", matchIfMissing = false)
public class TelegramVinculacionConfirmationListener {

    private final TelegramBotClient telegramBotClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarConfirmacion(TelegramVinculacionConfirmadaEvent event) {
        try {
            telegramBotClient.sendMessage(event.chatId(), crearMensaje(event.nombreEstudiante()));
        } catch (RuntimeException ex) {
            // Defensa adicional: un cliente inesperadamente defectuoso tampoco debe afectar el polling.
            log.warn("No fue posible enviar la confirmación de vinculación por Telegram. Detalle oculto por seguridad.");
            log.debug("Detalle del error de confirmación Telegram: {}", ex.getClass().getName());
        }
    }

    static String crearMensaje(String nombreEstudiante) {
        String encabezado = "🎓 AulaIA\n\n✅ Telegram vinculado correctamente";
        if (nombreEstudiante == null || nombreEstudiante.isBlank()) {
            return encabezado + ".\n\nA partir de ahora recibirás notificaciones cuando el estudiante registre su asistencia.";
        }
        return encabezado + " con " + nombreEstudiante.trim()
                + ".\n\nA partir de ahora recibirás notificaciones cuando registre su asistencia.";
    }
}
