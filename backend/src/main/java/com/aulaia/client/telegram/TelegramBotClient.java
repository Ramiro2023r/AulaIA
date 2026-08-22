package com.aulaia.client.telegram;

import com.aulaia.client.telegram.dto.TelegramUpdateDto;
import com.aulaia.client.telegram.dto.TelegramUpdatesResponse;
import com.aulaia.config.TelegramProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true", matchIfMissing = false)
public class TelegramBotClient {

    private final RestClient restClient;

    public TelegramBotClient(@Qualifier("telegramRestClient") RestClient telegramRestClient) {
        this.restClient = telegramRestClient;
    }

    public List<TelegramUpdateDto> getUpdates(Long offset) {
        try {
            String url = "/getUpdates";
            if (offset != null) {
                url += "?offset=" + offset;
            }

            TelegramUpdatesResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(TelegramUpdatesResponse.class);

            if (response != null && response.isOk() && response.getResult() != null) {
                return response.getResult();
            }
            
            log.warn("Telegram returned non-ok response or empty result");
            return Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Technical error connecting to Telegram API. (Error sanitized to protect token)");
            log.debug("Original error type: {}", e.getClass().getName());
            // Manejamos el error localmente para no tumbar la aplicación.
            return Collections.emptyList();
        }
    }

    /**
     * Envía un mensaje al chat ya validado por el flujo de vinculación.
     * Los errores externos se absorben para no afectar ninguna transacción
     * ni el ciclo de polling.
     */
    public void sendMessage(Long chatId, String text) {
        if (chatId == null || text == null || text.isBlank()) {
            return;
        }

        try {
            restClient.post()
                    .uri("/sendMessage")
                    .body(Map.of("chat_id", chatId, "text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            // No incluir URL, token ni chat_id: Telegram forma la URL base con el secreto.
            log.warn("No fue posible enviar un mensaje mediante Telegram. Detalle oculto por seguridad.");
            log.debug("Detalle del error de envío Telegram: {}", ex.getClass().getName());
        }
    }
}
