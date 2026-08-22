package com.aulaia.dto.telegram;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TelegramVinculacionLinkResponse {
    private String status;
    private String telegramUrl;
    private String expiresAt;
}
