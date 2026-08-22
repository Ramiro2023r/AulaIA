package com.aulaia.client.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TelegramUpdateDto {
    @JsonProperty("update_id")
    private Long updateId;
    private TelegramMessageDto message;
}
