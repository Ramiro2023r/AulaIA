package com.aulaia.service;

import com.aulaia.client.telegram.TelegramBotClient;
import com.aulaia.client.telegram.dto.TelegramUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true", matchIfMissing = false)
public class TelegramPollingService {

    /** Acepta solo el comando Telegram /start con un único parámetro opaco. */
    private static final Pattern START_COMMAND = Pattern.compile("^/start(?:\\s+(\\S+))?\\s*$");

    private final TelegramBotClient telegramBotClient;
    private final TelegramVinculacionService telegramVinculacionService;
    private Long offset = null;

    @Scheduled(fixedDelayString = "${telegram.polling.interval-ms:3000}")
    public void pollTelegramUpdates() {
        try {
            List<TelegramUpdateDto> updates = telegramBotClient.getUpdates(offset);
            if (updates != null && !updates.isEmpty()) {
                Long maxUpdateId = offset == null ? null : offset - 1;

                for (TelegramUpdateDto update : updates) {
                    try {
                        if (update == null || update.getUpdateId() == null) {
                            log.warn("Telegram update inválido recibido; se omitirá sin interrumpir el polling.");
                            continue;
                        }

                        maxUpdateId = maxUpdateId == null
                                ? update.getUpdateId()
                                : Math.max(maxUpdateId, update.getUpdateId());
                        procesarUpdate(update);
                    } catch (Exception ex) {
                        // Cada update es independiente: un fallo nunca bloquea los demás.
                        log.warn("Error procesando update de Telegram. Se continuará con los demás updates.");
                        log.debug("Detalle del error de update: {}", ex.getClass().getName());
                    }
                }

                if (maxUpdateId != null && maxUpdateId < Long.MAX_VALUE) {
                    offset = maxUpdateId + 1;
                }
            }
        } catch (Exception e) {
            log.warn("Error durante el polling de Telegram. Se reintentará en el próximo ciclo. Detalle oculto por seguridad.");
            log.debug("Detalle del error de polling: {}", e.getClass().getName());
        }
    }

    
    // Package-private o public getter/setter para testing
    public Long getOffset() {
        return offset;
    }
    
    public void setOffset(Long offset) {
        this.offset = offset;
    }

    private void procesarUpdate(TelegramUpdateDto update) {
        if (update.getMessage() == null) {
            return;
        }

        Optional<String> token = extraerTokenStart(update.getMessage().getText());
        if (token.isEmpty()) {
            return;
        }

        if (update.getMessage().getChat() == null || update.getMessage().getChat().getId() == null) {
            log.warn("Comando /start recibido sin chat válido; se omitirá el updateId={}", update.getUpdateId());
            return;
        }

        try {
            telegramVinculacionService.procesarComandoStart(token.get(), update.getMessage().getChat().getId());
        } catch (Exception ex) {
            log.warn("Error procesando comando /start para updateId={}. Detalle oculto por seguridad.", update.getUpdateId());
            log.debug("Detalle del error al procesar /start: {}", ex.getClass().getName());
        }
    }

    /**
     * Devuelve solo el argumento de un comando /start válido. Mensajes ajenos,
     * texto nulo y /start sin parámetro se ignoran sin invocar al servicio.
     */
    static Optional<String> extraerTokenStart(String text) {
        if (text == null) {
            return Optional.empty();
        }
        Matcher matcher = START_COMMAND.matcher(text.trim());
        if (!matcher.matches() || matcher.group(1) == null) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }
}
