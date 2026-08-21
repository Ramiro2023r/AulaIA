package com.aulaia.service;

import com.aulaia.dto.justificacion.EvaluarJustificacionRequest;
import com.aulaia.dto.justificacion.JustificacionRequest;
import com.aulaia.dto.justificacion.JustificacionResponse;
import com.aulaia.entity.Asistencia;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.EstadoJustificacion;
import com.aulaia.entity.Justificacion;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.BusinessException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.JustificacionMapper;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.JustificacionRepository;
import com.aulaia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class JustificacionServiceTest {

    @Mock
    private JustificacionRepository justificacionRepository;
    @Mock
    private AsistenciaRepository asistenciaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JustificacionMapper justificacionMapper;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private JustificacionService justificacionService;

    private Asistencia asistencia;
    private Usuario revisor;
    private Justificacion justificacion;

    @BeforeEach
    void setUp() {
        asistencia = new Asistencia();
        asistencia.setId(100L);
        asistencia.setEstado(EstadoAsistencia.AUSENTE);

        revisor = new Usuario();
        revisor.setId(10L);
        revisor.setUsername("admin");

        justificacion = new Justificacion();
        justificacion.setId(1L);
        justificacion.setAsistencia(asistencia);
        justificacion.setEstado(EstadoJustificacion.PENDIENTE);
        justificacion.setMotivo("Motivo test");
    }

    @Test
    void crear_Exito() {
        // Arrange
        JustificacionRequest request = new JustificacionRequest(100L, "Enfermedad");
        when(asistenciaRepository.findById(100L)).thenReturn(Optional.of(asistencia));
        when(justificacionRepository.findByAsistenciaId(100L)).thenReturn(Optional.empty());
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(revisor));
        when(justificacionRepository.save(any(Justificacion.class))).thenAnswer(i -> {
            Justificacion j = i.getArgument(0);
            j.setId(1L);
            return j;
        });
        when(justificacionMapper.toResponse(any())).thenReturn(new JustificacionResponse(1L, 100L, "Juan", "Perez", "Matematicas", java.time.LocalDate.now(), com.aulaia.entity.EstadoAsistencia.AUSENTE, "Enfermedad", EstadoJustificacion.PENDIENTE, null, null, null));

        // Act
        JustificacionResponse response = justificacionService.crear(request, "admin");

        // Assert
        assertThat(response).isNotNull();
        verify(justificacionRepository).save(any(Justificacion.class));
        verify(auditService).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void crear_Duplicado_LanzaException() {
        // Arrange
        JustificacionRequest request = new JustificacionRequest(100L, "Enfermedad");
        when(asistenciaRepository.findById(100L)).thenReturn(Optional.of(asistencia));
        when(justificacionRepository.findByAsistenciaId(100L)).thenReturn(Optional.of(justificacion));

        // Act & Assert
        assertThatThrownBy(() -> justificacionService.crear(request, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe una justificación");
    }

    @Test
    void evaluar_Aprobada_ActualizaAsistencia() {
        // Arrange
        EvaluarJustificacionRequest request = new EvaluarJustificacionRequest(EstadoJustificacion.APROBADA);
        when(justificacionRepository.findById(1L)).thenReturn(Optional.of(justificacion));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(revisor));
        when(justificacionRepository.save(any(Justificacion.class))).thenReturn(justificacion);
        when(justificacionMapper.toResponse(any())).thenReturn(new JustificacionResponse(1L, 100L, "Juan", "Perez", "Matematicas", java.time.LocalDate.now(), com.aulaia.entity.EstadoAsistencia.AUSENTE, "Motivo test", EstadoJustificacion.APROBADA, "admin", null, null));

        // Act
        justificacionService.evaluar(1L, request, "admin");

        // Assert
        assertThat(justificacion.getEstado()).isEqualTo(EstadoJustificacion.APROBADA);
        assertThat(asistencia.getEstado()).isEqualTo(EstadoAsistencia.JUSTIFICADO);
        verify(asistenciaRepository).save(asistencia);
        verify(justificacionRepository).save(justificacion);
    }
}
