package com.aulaia.service;

import com.aulaia.dto.TelegramVinculacionResponseDto;
import com.aulaia.entity.Apoderado;
import com.aulaia.entity.EstadoVinculacion;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.EstudianteApoderado;
import com.aulaia.entity.Parentesco;
import com.aulaia.entity.TelegramVinculacion;
import com.aulaia.exception.BusinessException;
import com.aulaia.repository.ApoderadoRepository;
import com.aulaia.repository.EstudianteApoderadoRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.TelegramVinculacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TelegramVinculacionServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-22T12:00:00Z");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 8, 22, 12, 0);

    @Mock
    private TelegramVinculacionRepository vinculacionRepository;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private ApoderadoRepository apoderadoRepository;
    @Mock
    private EstudianteApoderadoRepository estudianteApoderadoRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    private TelegramVinculacionService vinculacionService;

    private Estudiante estudiante;
    private Apoderado apoderado;

    @BeforeEach
    void setUp() {
        vinculacionService = new TelegramVinculacionService(
                vinculacionRepository,
                estudianteRepository,
                apoderadoRepository,
                estudianteApoderadoRepository,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC),
                eventPublisher);
        estudiante = new Estudiante();
        estudiante.setId(1L);
        estudiante.setNombres("Juan");
        estudiante.setApellidos("Pérez");

        apoderado = new Apoderado();
        apoderado.setId(2L);
        apoderado.setNombres("Maria");
    }

    @Test
    void crearVinculacionSinApoderadoEsRechazada() {
        assertThatThrownBy(() -> vinculacionService.crearVinculacion(1L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("seleccionar un apoderado");
        verifyNoInteractions(vinculacionRepository, estudianteRepository, apoderadoRepository,
                estudianteApoderadoRepository);
    }

    @Test
    void test1_CrearVinculacionValidaConApoderado() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(apoderadoRepository.findById(2L)).thenReturn(Optional.of(apoderado));
        
        EstudianteApoderado relacion = new EstudianteApoderado();
        relacion.setEstudiante(estudiante);
        relacion.setApoderado(apoderado);
        relacion.setParentesco(Parentesco.MADRE);
        when(estudianteApoderadoRepository.findByEstudianteId(1L)).thenReturn(List.of(relacion));
        
        when(vinculacionRepository.findByEstudianteIdAndEstado(1L, EstadoVinculacion.PENDIENTE)).thenReturn(List.of());

        TelegramVinculacionResponseDto response = vinculacionService.crearVinculacion(1L, 2L);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getEstado()).isEqualTo(EstadoVinculacion.PENDIENTE);
        
        ArgumentCaptor<TelegramVinculacion> vinculacionCaptor = ArgumentCaptor.forClass(TelegramVinculacion.class);
        verify(vinculacionRepository).save(vinculacionCaptor.capture());
        assertThat(vinculacionCaptor.getValue().getApoderado()).isSameAs(apoderado);
    }

    @Test
    void test2_TokenUnicoYFuerte() {
        prepararApoderadoRelacionado();
        when(vinculacionRepository.findByEstudianteIdAndEstado(1L, EstadoVinculacion.PENDIENTE)).thenReturn(List.of());

        TelegramVinculacionResponseDto resp1 = vinculacionService.crearVinculacion(1L, 2L);
        TelegramVinculacionResponseDto resp2 = vinculacionService.crearVinculacion(1L, 2L);

        assertThat(resp1.getToken()).isNotEqualTo(resp2.getToken());
        // El token base64 de 32 bytes debe medir 43 caracteres sin padding
        assertThat(resp1.getToken().length()).isEqualTo(43); 
    }


    @Test
    void procesarComandoStart_Success() { // 1. vinculación correcta, 2. guarda chat_id, 3. cambia estado, 4. marca usedAt
        TelegramVinculacion vinculacion = new TelegramVinculacion();
        vinculacion.setToken("VALID_TOKEN");
        vinculacion.setEstado(EstadoVinculacion.PENDIENTE);
        vinculacion.setExpiresAt(FIXED_NOW.plusHours(1));
        vinculacion.setEstudiante(estudiante);

        Apoderado apoderado = new Apoderado();
        vinculacion.setApoderado(apoderado);

        when(vinculacionRepository.findByTokenForUpdate("VALID_TOKEN")).thenReturn(Optional.of(vinculacion));

        vinculacionService.procesarComandoStart("VALID_TOKEN", 123456L);

        assertThat(vinculacion.getEstado()).isEqualTo(EstadoVinculacion.VINCULADO);
        assertThat(vinculacion.getUsedAt()).isNotNull();
        assertThat(apoderado.getTelegramChatId()).isEqualTo("123456");
        assertThat(apoderado.getTelegramVinculadoAt()).isNotNull();

        verify(apoderadoRepository).save(apoderado);
        verify(vinculacionRepository).save(vinculacion);
        org.mockito.ArgumentCaptor<TelegramVinculacionConfirmadaEvent> eventCaptor =
                org.mockito.ArgumentCaptor.forClass(TelegramVinculacionConfirmadaEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().chatId()).isEqualTo(123456L);
        assertThat(eventCaptor.getValue().nombreEstudiante()).isEqualTo("Juan Pérez");
    }

    @Test
    void procesarComandoStart_TokenInexistente_Ignores() { // 5. token inexistente
        when(vinculacionRepository.findByTokenForUpdate("INVALID_TOKEN")).thenReturn(Optional.empty());

        vinculacionService.procesarComandoStart("INVALID_TOKEN", 123456L);

        verify(vinculacionRepository, never()).save(any());
        verify(apoderadoRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void procesarComandoStart_TokenExpirado_ChangesStateAndIgnores() { // 6. token expirado
        TelegramVinculacion vinculacion = new TelegramVinculacion();
        vinculacion.setToken("EXPIRED_TOKEN");
        vinculacion.setEstado(EstadoVinculacion.PENDIENTE);
        vinculacion.setExpiresAt(FIXED_NOW.minusHours(1)); // Expiró hace 1 hora

        when(vinculacionRepository.findByTokenForUpdate("EXPIRED_TOKEN")).thenReturn(Optional.of(vinculacion));

        vinculacionService.procesarComandoStart("EXPIRED_TOKEN", 123456L);

        assertThat(vinculacion.getEstado()).isEqualTo(EstadoVinculacion.EXPIRADO);
        verify(vinculacionRepository).save(vinculacion);
        verify(apoderadoRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void procesarComandoStart_TokenRevocado_Ignores() { // 7. token revocado
        TelegramVinculacion vinculacion = new TelegramVinculacion();
        vinculacion.setToken("REVOKED_TOKEN");
        vinculacion.setEstado(EstadoVinculacion.REVOCADO);
        vinculacion.setExpiresAt(FIXED_NOW.plusHours(1));

        when(vinculacionRepository.findByTokenForUpdate("REVOKED_TOKEN")).thenReturn(Optional.of(vinculacion));

        vinculacionService.procesarComandoStart("REVOKED_TOKEN", 123456L);

        verify(vinculacionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void procesarComandoStart_TokenYaUsado_Ignores() { // 8. token ya usado
        TelegramVinculacion vinculacion = new TelegramVinculacion();
        vinculacion.setToken("USED_TOKEN");
        vinculacion.setEstado(EstadoVinculacion.VINCULADO);
        vinculacion.setExpiresAt(FIXED_NOW.plusHours(1));

        when(vinculacionRepository.findByTokenForUpdate("USED_TOKEN")).thenReturn(Optional.of(vinculacion));

        vinculacionService.procesarComandoStart("USED_TOKEN", 123456L);

        verify(vinculacionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void procesarComandoStart_NullOrEmptyToken_Ignores() {
        vinculacionService.procesarComandoStart(null, 123456L);
        vinculacionService.procesarComandoStart("", 123456L);
        vinculacionService.procesarComandoStart("   ", 123456L);

        verify(vinculacionRepository, never()).findByTokenForUpdate(any());
    }

    @Test
    void procesarComandoStart_ExpiraCuandoLaInvitacionVenceExactamenteAhora() {
        TelegramVinculacion vinculacion = new TelegramVinculacion();
        vinculacion.setEstado(EstadoVinculacion.PENDIENTE);
        vinculacion.setExpiresAt(FIXED_NOW);
        when(vinculacionRepository.findByTokenForUpdate("TOKEN_AL_LIMITE"))
                .thenReturn(Optional.of(vinculacion));

        vinculacionService.procesarComandoStart("TOKEN_AL_LIMITE", 123456L);

        assertThat(vinculacion.getEstado()).isEqualTo(EstadoVinculacion.EXPIRADO);
        assertThat(vinculacion.getUsedAt()).isNull();
        verify(apoderadoRepository, never()).save(any());
    }

    @Test
    void test4_EstudianteInexistente() {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vinculacionService.crearVinculacion(99L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estudiante no encontrado");
    }

    @Test
    void test5_ApoderadoInexistente() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(apoderadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vinculacionService.crearVinculacion(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Apoderado no encontrado");
    }

    @Test
    void test6_ApoderadoNoRelacionadoConEstudiante() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(apoderadoRepository.findById(2L)).thenReturn(Optional.of(apoderado));
        
        // Retornar lista vacía de relaciones para este estudiante
        when(estudianteApoderadoRepository.findByEstudianteId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> vinculacionService.crearVinculacion(1L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apoderado no está relacionado");
    }

    @Test
    void test8_RevocacionOReemplazoDeVinculacionPendiente() {
        prepararApoderadoRelacionado();
        
        TelegramVinculacion pendienteAnterior = new TelegramVinculacion();
        pendienteAnterior.setToken("tokenViejo123");
        pendienteAnterior.setEstado(EstadoVinculacion.PENDIENTE);
        
        when(vinculacionRepository.findByEstudianteIdAndEstado(1L, EstadoVinculacion.PENDIENTE))
                .thenReturn(List.of(pendienteAnterior));

        TelegramVinculacionResponseDto nueva = vinculacionService.crearVinculacion(1L, 2L);

        // Verifica que la anterior se haya cambiado a REVOCADO y guardado
        assertThat(pendienteAnterior.getEstado()).isEqualTo(EstadoVinculacion.REVOCADO);
        verify(vinculacionRepository).save(pendienteAnterior);
        
        // Verifica que el nuevo token es distinto
        assertThat(nueva.getToken()).isNotEqualTo("tokenViejo123");
    }

    @Test
    void apoderadoInactivoNoPuedeRecibirUnaVinculacion() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        apoderado.setActivo(false);
        when(apoderadoRepository.findById(2L)).thenReturn(Optional.of(apoderado));

        assertThatThrownBy(() -> vinculacionService.crearVinculacion(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactivo");
        verify(estudianteApoderadoRepository, never()).findByEstudianteId(any());
        verify(vinculacionRepository, never()).save(any());
    }

    private void prepararApoderadoRelacionado() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(apoderadoRepository.findById(2L)).thenReturn(Optional.of(apoderado));
        EstudianteApoderado relacion = new EstudianteApoderado();
        relacion.setEstudiante(estudiante);
        relacion.setApoderado(apoderado);
        relacion.setParentesco(Parentesco.MADRE);
        when(estudianteApoderadoRepository.findByEstudianteId(1L)).thenReturn(List.of(relacion));
    }
}
