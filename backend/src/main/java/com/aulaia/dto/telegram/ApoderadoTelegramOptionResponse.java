package com.aulaia.dto.telegram;

/** Datos mínimos para elegir el apoderado destinatario de una vinculación Telegram. */
public record ApoderadoTelegramOptionResponse(
        Long id,
        String nombres,
        String apellidos,
        String parentesco,
        boolean principal,
        boolean activo) {
}
