package com.aulaia.client.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Health check response de FastAPI (Prompt 16.5).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastApiHealthResponse {

    private String status;

    private String service;

    @JsonProperty("module")
    private String module;

    private String version;
}