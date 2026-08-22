package com.aulaia.dto;

import com.aulaia.client.fastapi.dto.FastApiAnalisisResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response de consulta IA (Prompt 17.1).
 * <p>
 * Incluye la respuesta en texto natural, flag de disponibilidad,
 * y opcionalmente los datos estructurados del análisis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IaConsultaResponse {

    private String respuesta;
    
    private boolean iaDisponible;
    
    // Datos estructurados opcionales para frontend avanzado
    private FastApiAnalisisResponse datosAnalisis;
}