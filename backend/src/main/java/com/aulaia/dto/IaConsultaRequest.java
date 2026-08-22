package com.aulaia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para consulta en lenguaje natural a la IA (Prompt 17.1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IaConsultaRequest {

    private String pregunta;
    
    // Opcional: contexto adicional (sección, periodo, etc.)
    private String contexto;
}