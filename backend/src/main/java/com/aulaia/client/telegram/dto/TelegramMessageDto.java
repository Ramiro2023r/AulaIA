package com.aulaia.client.telegram.dto;

import lombok.Data;

@Data
public class TelegramMessageDto {
    private TelegramUserDto from;
    private TelegramChatDto chat;
    private String text;
}
