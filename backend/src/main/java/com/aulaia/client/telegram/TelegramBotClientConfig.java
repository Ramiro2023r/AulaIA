package com.aulaia.client.telegram;

import com.aulaia.config.TelegramProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true", matchIfMissing = false)
public class TelegramBotClientConfig {

    @Bean
    public RestClient telegramRestClient(RestClient.Builder builder, TelegramProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);

        String token = properties.getBot().getToken();
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Telegram bot token is not configured");
        }

        return builder
                .baseUrl("https://api.telegram.org/bot" + token)
                .requestFactory(factory)
                .build();
    }
}
