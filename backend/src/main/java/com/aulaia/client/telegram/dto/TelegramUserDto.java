package com.aulaia.client.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TelegramUserDto {
    private Long id;
    @JsonProperty("first_name")
    private String firstName;
    private String username;
}
