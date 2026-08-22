package com.aulaia.client.telegram.dto;

import lombok.Data;
import java.util.List;

@Data
public class TelegramUpdatesResponse {
    private boolean ok;
    private List<TelegramUpdateDto> result;
}
