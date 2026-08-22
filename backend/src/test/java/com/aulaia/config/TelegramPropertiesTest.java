package com.aulaia.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {TelegramProperties.class})
@EnableConfigurationProperties(TelegramProperties.class)
@TestPropertySource(properties = {
        "telegram.bot.token=test_token",
        "telegram.bot.username=test_bot",
        "telegram.enabled=true"
})
public class TelegramPropertiesTest {

    @Autowired
    private TelegramProperties telegramProperties;

    @Test
    void testTelegramPropertiesAreLoaded() {
        assertNotNull(telegramProperties, "TelegramProperties no debe ser nulo");
        assertTrue(telegramProperties.isEnabled(), "La propiedad telegram.enabled debe ser true");
        assertNotNull(telegramProperties.getBot(), "Bot properties no deben ser nulas");
        assertEquals("test_token", telegramProperties.getBot().getToken());
        assertEquals("test_bot", telegramProperties.getBot().getUsername());
    }
}
