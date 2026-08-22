package com.aulaia.service;

/**
 * Evento interno emitido al confirmar una vinculación. Contiene solo el
 * destino Telegram y el nombre que puede mostrarse de forma acotada.
 */
public record TelegramVinculacionConfirmadaEvent(Long chatId, String nombreEstudiante) {
}
