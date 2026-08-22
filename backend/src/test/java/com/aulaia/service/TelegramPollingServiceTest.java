package com.aulaia.service;

import com.aulaia.client.telegram.TelegramBotClient;
import com.aulaia.client.telegram.dto.TelegramChatDto;
import com.aulaia.client.telegram.dto.TelegramMessageDto;
import com.aulaia.client.telegram.dto.TelegramUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramPollingServiceTest {

    @Mock
    private TelegramBotClient telegramBotClient;

    @Mock
    private TelegramVinculacionService telegramVinculacionService;

    @InjectMocks
    private TelegramPollingService pollingService;

    @BeforeEach
    void setUp() {
        pollingService.setOffset(null);
    }

    private TelegramUpdateDto createUpdate(Long updateId, String text, Long chatId) {
        TelegramUpdateDto update = new TelegramUpdateDto();
        update.setUpdateId(updateId);
        if (text != null || chatId != null) {
            TelegramMessageDto message = new TelegramMessageDto();
            message.setText(text);
            if (chatId != null) {
                TelegramChatDto chat = new TelegramChatDto();
                chat.setId(chatId);
                message.setChat(chat);
            }
            update.setMessage(message);
        }
        return update;
    }

    @Test
    void pollTelegramUpdates_EmptyUpdate_OffsetNotChanged() { // 3. update vacío
        when(telegramBotClient.getUpdates(null)).thenReturn(Collections.emptyList());

        pollingService.pollTelegramUpdates();

        assertThat(pollingService.getOffset()).isNull();
        verify(telegramBotClient, times(1)).getUpdates(null);
    }

    @Test
    void pollTelegramUpdates_OneUpdateWithStart_ProcessesAndUpdatesOffset() { // 4. un update actualiza offset
        TelegramUpdateDto update = createUpdate(100L, "/start MI_TOKEN", 12345L);
        when(telegramBotClient.getUpdates(null)).thenReturn(List.of(update));

        pollingService.pollTelegramUpdates();

        assertThat(pollingService.getOffset()).isEqualTo(101L); // 100 + 1
        verify(telegramVinculacionService).procesarComandoStart("MI_TOKEN", 12345L);
    }

    @Test
    void pollTelegramUpdates_MultipleUpdates_LeavesOffsetAtMaxPlusOne() { // 5. varios updates dejan offset en el mayor updateId + 1 (14. offset usa max+1)
        TelegramUpdateDto update1 = createUpdate(100L, "texto random", 1L);
        TelegramUpdateDto update2 = createUpdate(105L, "/start TOKEN", 2L);
        TelegramUpdateDto update3 = createUpdate(101L, "otro texto", 3L);
        
        when(telegramBotClient.getUpdates(null)).thenReturn(Arrays.asList(update1, update2, update3));

        pollingService.pollTelegramUpdates();

        assertThat(pollingService.getOffset()).isEqualTo(106L); // max(100, 105, 101) + 1
        verify(telegramVinculacionService).procesarComandoStart("TOKEN", 2L);
    }

    @Test
    void pollTelegramUpdates_DuplicateUpdates_OffsetContinuesCorrectly() { // 6. update repetido no se reprocesa
        TelegramUpdateDto update1 = createUpdate(50L, "/start ABC", 1L);
        when(telegramBotClient.getUpdates(null)).thenReturn(List.of(update1));
        pollingService.pollTelegramUpdates();
        assertThat(pollingService.getOffset()).isEqualTo(51L);
        
        when(telegramBotClient.getUpdates(51L)).thenReturn(Collections.emptyList());
        pollingService.pollTelegramUpdates();
        assertThat(pollingService.getOffset()).isEqualTo(51L); // Sigue igual
        
        verify(telegramBotClient, times(1)).getUpdates(null);
        verify(telegramBotClient, times(1)).getUpdates(51L);
    }

    @Test
    void pollTelegramUpdates_TelegramFails_DoesNotStopPollingAndKeepsOffset() { // 7. error de Telegram no detiene polling
        when(telegramBotClient.getUpdates(null)).thenThrow(new RuntimeException("Error simulado de Telegram"));

        pollingService.pollTelegramUpdates();

        assertThat(pollingService.getOffset()).isNull();
    }

    @Test
    void pollTelegramUpdates_ProcessingFails_DoesNotStopPollingAndUpdatesOffset() { // 13. error en procesamiento no detiene polling
        TelegramUpdateDto update = createUpdate(200L, "/start ERROR", 999L);
        TelegramUpdateDto siguienteUpdate = createUpdate(201L, "/start VALIDO", 1000L);
        when(telegramBotClient.getUpdates(null)).thenReturn(List.of(update, siguienteUpdate));
        doThrow(new RuntimeException("Error bd")).when(telegramVinculacionService).procesarComandoStart("ERROR", 999L);

        pollingService.pollTelegramUpdates();

        // El error del primer update no impide procesar el siguiente ni avanzar el offset.
        assertThat(pollingService.getOffset()).isEqualTo(202L);
        verify(telegramVinculacionService).procesarComandoStart("VALIDO", 1000L);
    }

    @Test
    void pollTelegramUpdates_StartWithoutToken_DoesNotProcess() { // 9. /start sin token
        TelegramUpdateDto update = createUpdate(300L, "/start", 1L);
        TelegramUpdateDto update2 = createUpdate(301L, "/start   ", 2L);
        when(telegramBotClient.getUpdates(null)).thenReturn(List.of(update, update2));

        pollingService.pollTelegramUpdates();

        assertThat(pollingService.getOffset()).isEqualTo(302L);
        verifyNoInteractions(telegramVinculacionService);
    }

    @Test
    void pollTelegramUpdates_UpdateWithoutMessage_TextOrChat() { // 11. sin message, 12. sin chat, texto nulo
        TelegramUpdateDto update1 = new TelegramUpdateDto();
        update1.setUpdateId(400L); // sin message
        
        TelegramUpdateDto update2 = createUpdate(401L, "/start OK", null); // sin chat

        TelegramUpdateDto update3 = createUpdate(402L, null, 3L); // texto nulo
        
        when(telegramBotClient.getUpdates(null)).thenReturn(List.of(update1, update2, update3));

        pollingService.pollTelegramUpdates();

        assertThat(pollingService.getOffset()).isEqualTo(403L);
        verifyNoInteractions(telegramVinculacionService);
    }

    @Test
    void extraerTokenStart_AceptaSoloComandoConUnToken() {
        assertThat(TelegramPollingService.extraerTokenStart("/start TOKEN_VALIDO"))
                .contains("TOKEN_VALIDO");
        assertThat(TelegramPollingService.extraerTokenStart("/start")).isEmpty();
        assertThat(TelegramPollingService.extraerTokenStart("/start TOKEN otro")).isEmpty();
        assertThat(TelegramPollingService.extraerTokenStart("mensaje /start TOKEN")).isEmpty();
        assertThat(TelegramPollingService.extraerTokenStart(null)).isEmpty();
    }
}
