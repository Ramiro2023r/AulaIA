package com.aulaia.service;

import com.aulaia.client.telegram.TelegramBotClient;
import com.aulaia.entity.Apoderado;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.EstudianteApoderado;
import com.aulaia.repository.EstudianteApoderadoRepository;
import com.aulaia.repository.EstudianteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramAsistenciaNotificationListenerTest {

    private static final OffsetDateTime FECHA_HORA = OffsetDateTime.of(2026, 8, 22, 9, 5, 0, 0, ZoneOffset.ofHours(-5));

    @Mock
    private EstudianteApoderadoRepository estudianteApoderadoRepository;
    @Mock
    private TelegramBotClient telegramBotClient;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private ObjectProvider<EstudianteRepository> estudianteRepositoryProvider;

    @Test
    void unApoderadoVinculadoRecibeUnMensajeConDatosReales() {
        when(estudianteApoderadoRepository.findWithApoderadoByEstudianteId(10L))
                .thenReturn(List.of(relacion("123456")));

        listener().notificarAsistencia(evento());

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(telegramBotClient).sendMessage(eq(123456L), mensaje.capture());
        assertThat(mensaje.getValue()).contains("Diego Pérez", "22/08/2026", "09:05", "Computación", "6.º A", "PRESENTE");
    }

    @Test
    void dosApoderadosVinculadosRecibenDosMensajes() {
        when(estudianteApoderadoRepository.findWithApoderadoByEstudianteId(10L))
                .thenReturn(List.of(relacion("111"), relacion("222")));

        listener().notificarAsistencia(evento());

        verify(telegramBotClient).sendMessage(eq(111L), anyString());
        verify(telegramBotClient).sendMessage(eq(222L), anyString());
    }

    @Test
    void recuperaElNombreRealSiElEventoLoRecibeComoUndefined() {
        when(estudianteApoderadoRepository.findWithApoderadoByEstudianteId(10L))
                .thenReturn(List.of(relacion("123456")));
        when(estudianteRepositoryProvider.getIfAvailable()).thenReturn(estudianteRepository);
        Estudiante estudiante = new Estudiante();
        estudiante.setNombres("Carlos");
        estudiante.setApellidos("Gómez");
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));

        listener().notificarAsistencia(new AsistenciaRegistradaEvent(1L, 10L, "undefined", FECHA_HORA,
                EstadoAsistencia.PRESENTE, "Computación", "6.º A"));

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(telegramBotClient).sendMessage(eq(123456L), mensaje.capture());
        assertThat(mensaje.getValue()).contains("Carlos Gómez").doesNotContain("undefined");
    }

    @Test
    void sinApoderadosVinculadosNoEnviaNada() {
        when(estudianteApoderadoRepository.findWithApoderadoByEstudianteId(10L)).thenReturn(List.of());

        listener().notificarAsistencia(evento());

        verify(telegramBotClient, never()).sendMessage(anyLong(), anyString());
    }

    @Test
    void apoderadoSinChatIdNoRecibeMensaje() {
        EstudianteApoderado relacion = relacion(null);
        when(estudianteApoderadoRepository.findWithApoderadoByEstudianteId(10L)).thenReturn(List.of(relacion));

        listener().notificarAsistencia(evento());

        verify(telegramBotClient, never()).sendMessage(anyLong(), anyString());
    }

    @Test
    void falloDeUnApoderadoNoDetieneElSiguienteNiPropagaError() {
        when(estudianteApoderadoRepository.findWithApoderadoByEstudianteId(10L))
                .thenReturn(List.of(relacion("111"), relacion("222")));
        doThrow(new RuntimeException("fallo externo")).when(telegramBotClient).sendMessage(eq(111L), anyString());

        assertThatCode(() -> listener().notificarAsistencia(evento())).doesNotThrowAnyException();

        verify(telegramBotClient).sendMessage(eq(222L), anyString());
    }

    @Test
    void noDuplicaEnvioParaElMismoChatEnLaMismaEjecucion() {
        when(estudianteApoderadoRepository.findWithApoderadoByEstudianteId(10L))
                .thenReturn(List.of(relacion("123456"), relacion("123456")));

        listener().notificarAsistencia(evento());

        verify(telegramBotClient).sendMessage(eq(123456L), anyString());
    }

    @Test
    void listenerEstaConfiguradoParaEjecutarseDespuesDelCommit() throws NoSuchMethodException {
        Method method = TelegramAsistenciaNotificationListener.class
                .getMethod("notificarAsistencia", AsistenciaRegistradaEvent.class);
        assertThat(method.getAnnotation(TransactionalEventListener.class).phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    private TelegramAsistenciaNotificationListener listener() {
        return new TelegramAsistenciaNotificationListener(
                estudianteApoderadoRepository, telegramBotClient, estudianteRepositoryProvider);
    }

    private AsistenciaRegistradaEvent evento() {
        return new AsistenciaRegistradaEvent(1L, 10L, "Diego Pérez", FECHA_HORA,
                EstadoAsistencia.PRESENTE, "Computación", "6.º A");
    }

    private EstudianteApoderado relacion(String chatId) {
        Apoderado apoderado = new Apoderado();
        apoderado.setActivo(true);
        apoderado.setTelegramChatId(chatId);
        apoderado.setTelegramVinculadoAt(FECHA_HORA.minusDays(1));
        EstudianteApoderado relacion = new EstudianteApoderado();
        relacion.setApoderado(apoderado);
        return relacion;
    }
}
