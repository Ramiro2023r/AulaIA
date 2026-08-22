package com.aulaia.dto;

import com.aulaia.entity.EstadoVinculacion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TelegramVinculacionResponseDto {
    private String token;
    private EstadoVinculacion estado;
    private LocalDateTime expiresAt;
}
