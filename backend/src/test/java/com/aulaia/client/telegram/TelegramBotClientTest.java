package com.aulaia.client.telegram;

import com.aulaia.client.telegram.dto.TelegramUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.test.web.client.ExpectedCount;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TelegramBotClientTest {

    private TelegramBotClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org/botmock-token-123");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        client = new TelegramBotClient(restClient);
    }

    @Test
    void getUpdates_WithZeroUpdates_ReturnsEmptyList() {
        String jsonResponse = "{\"ok\":true,\"result\":[]}";
        mockServer.expect(requestTo("https://api.telegram.org/botmock-token-123/getUpdates"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<TelegramUpdateDto> updates = client.getUpdates(null);
        assertThat(updates).isEmpty();
        mockServer.verify();
    }

    @Test
    void getUpdates_WithUpdates_ExtractsCorrectly() {
        String jsonResponse = """
            {
              "ok": true,
              "result": [
                {
                  "update_id": 12345,
                  "message": {
                    "text": "/start mytoken",
                    "chat": {
                      "id": 987654
                    },
                    "from": {
                      "id": 111,
                      "first_name": "Juan",
                      "username": "juan123"
                    }
                  }
                }
              ]
            }
            """;
        mockServer.expect(requestTo("https://api.telegram.org/botmock-token-123/getUpdates"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<TelegramUpdateDto> updates = client.getUpdates(null);
        assertThat(updates).hasSize(1);
        TelegramUpdateDto update = updates.get(0);
        assertThat(update.getUpdateId()).isEqualTo(12345L);
        assertThat(update.getMessage().getText()).isEqualTo("/start mytoken");
        assertThat(update.getMessage().getChat().getId()).isEqualTo(987654L);
        assertThat(update.getMessage().getFrom().getId()).isEqualTo(111L);
        assertThat(update.getMessage().getFrom().getFirstName()).isEqualTo("Juan");
        assertThat(update.getMessage().getFrom().getUsername()).isEqualTo("juan123");
        mockServer.verify();
    }

    @Test
    void getUpdates_WithOffset_AppendsOffsetParameter() {
        String jsonResponse = "{\"ok\":true,\"result\":[]}";
        mockServer.expect(requestTo("https://api.telegram.org/botmock-token-123/getUpdates?offset=12346"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        client.getUpdates(12346L);
        mockServer.verify();
    }

    @Test
    void getUpdates_ReturnsHttpError_HandledWithoutException() {
        mockServer.expect(requestTo("https://api.telegram.org/botmock-token-123/getUpdates"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        List<TelegramUpdateDto> updates = client.getUpdates(null);
        assertThat(updates).isEmpty();
        mockServer.verify();
    }
    
    @Test
    void getUpdates_Timeout_HandledWithoutException() {
        mockServer.expect(requestTo("https://api.telegram.org/botmock-token-123/getUpdates"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> { throw new IOException("Timeout"); });

        List<TelegramUpdateDto> updates = client.getUpdates(null);
        assertThat(updates).isEmpty();
        mockServer.verify();
    }

    @Test
    void getUpdates_ReturnsOkFalse_ReturnsEmptyList() {
        String jsonResponse = "{\"ok\":false,\"result\":[]}";
        mockServer.expect(requestTo("https://api.telegram.org/botmock-token-123/getUpdates"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<TelegramUpdateDto> updates = client.getUpdates(null);
        assertThat(updates).isEmpty();
        mockServer.verify();
    }

    @Test
    void sendMessage_EnviaPostConChatIdYTexto() {
        mockServer.expect(requestTo("https://api.telegram.org/botmock-token-123/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"chat_id\":123456789,\"text\":\"Confirmación AulaIA\"}"))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{}}", MediaType.APPLICATION_JSON));

        client.sendMessage(123456789L, "Confirmación AulaIA");

        mockServer.verify();
    }

    @Test
    void sendMessage_ErroresTelegramYTimeoutNoPropaganExcepcion() {
        for (HttpStatus status : List.of(HttpStatus.UNAUTHORIZED, HttpStatus.TOO_MANY_REQUESTS,
                HttpStatus.INTERNAL_SERVER_ERROR)) {
            mockServer.expect(ExpectedCount.once(), requestTo("https://api.telegram.org/botmock-token-123/sendMessage"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(status));
        }
        mockServer.expect(ExpectedCount.once(), requestTo("https://api.telegram.org/botmock-token-123/sendMessage"))
                .andRespond(request -> { throw new IOException("Timeout"); });

        client.sendMessage(123456789L, "Confirmación AulaIA");
        client.sendMessage(123456789L, "Confirmación AulaIA");
        client.sendMessage(123456789L, "Confirmación AulaIA");
        client.sendMessage(123456789L, "Confirmación AulaIA");

        mockServer.verify();
    }
}
