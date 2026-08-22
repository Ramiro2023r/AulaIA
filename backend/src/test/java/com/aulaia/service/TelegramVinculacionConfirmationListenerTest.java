package com.aulaia.service;

import com.aulaia.client.telegram.TelegramBotClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelegramVinculacionConfirmationListenerTest {

    @Mock
    private TelegramBotClient telegramBotClient;

    @Test
    void enviarConfirmacion_UsaElChatDelEventoYNombreDelEstudiante() {
        TelegramVinculacionConfirmationListener listener =
                new TelegramVinculacionConfirmationListener(telegramBotClient);

        listener.enviarConfirmacion(new TelegramVinculacionConfirmadaEvent(123456L, "Diego Pérez"));

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(telegramBotClient).sendMessage(org.mockito.ArgumentMatchers.eq(123456L), mensaje.capture());
        assertThat(mensaje.getValue()).contains("Telegram vinculado correctamente", "Diego Pérez");
    }

    @Test
    void enviarConfirmacion_SinNombreUsaMensajeGenerico() {
        TelegramVinculacionConfirmationListener listener =
                new TelegramVinculacionConfirmationListener(telegramBotClient);

        listener.enviarConfirmacion(new TelegramVinculacionConfirmadaEvent(123456L, null));

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(telegramBotClient).sendMessage(org.mockito.ArgumentMatchers.eq(123456L), mensaje.capture());
        assertThat(mensaje.getValue()).contains("Telegram vinculado correctamente")
                .doesNotContain(" con ");
    }

    @Test
    void enviarConfirmacion_FalloDelClienteNoPropagaError() {
        TelegramVinculacionConfirmationListener listener =
                new TelegramVinculacionConfirmationListener(telegramBotClient);
        doThrow(new RuntimeException("fallo externo")).when(telegramBotClient)
                .sendMessage(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());

        assertThatCode(() -> listener.enviarConfirmacion(
                new TelegramVinculacionConfirmadaEvent(123456L, "Diego Pérez"))).doesNotThrowAnyException();
    }
}
