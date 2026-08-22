package com.aulaia.client.fastapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import java.time.Duration;

/**
 * Configuración del cliente HTTP para FastAPI (Prompt 16.5).
 * <p>
 * El cliente se comunica con el servicio de IA desacoplado.
 * Timeouts configurables para evitar bloqueos en el backend principal.
 */
@Configuration
public class FastApiClientConfig {

    @Bean
    public RestClient fastApiRestClient(Builder builder,
                                        FastApiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        factory.setReadTimeout((int) properties.getReadTimeout().toMillis());

        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}