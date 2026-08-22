package com.aulaia.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.aulaia.client.telegram.TelegramBotClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TelegramPollingServiceDisabledIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withBean(TelegramPollingService.class)
            .withPropertyValues("telegram.enabled=false");

    @Configuration
    @EnableScheduling
    static class TestConfig {
        @Bean
        public TelegramBotClient telegramBotClient() {
            return mock(TelegramBotClient.class);
        }

        @Bean
        public TelegramVinculacionService telegramVinculacionService() {
            return mock(TelegramVinculacionService.class);
        }
    }

    @Test
    void telegramDisabled_ServiceBeanNotCreated() { // 1. Telegram deshabilitado → no consulta
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TelegramPollingService.class);
        });
    }
}
