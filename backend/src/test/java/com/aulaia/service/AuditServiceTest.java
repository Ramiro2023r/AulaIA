package com.aulaia.service;

import com.aulaia.entity.Auditoria;
import com.aulaia.entity.Usuario;
import com.aulaia.repository.AuditoriaRepository;
import com.aulaia.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void registrar_DebeGuardarAuditoriaConDatosYUsuario() throws Exception {
        // Arrange
        String username = "admin_user";
        Usuario usuarioMock = new Usuario();
        usuarioMock.setId(10L);
        usuarioMock.setUsername(username);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(username);
        when(authentication.getName()).thenReturn(username);
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuarioMock));

        Map<String, String> valorAnterior = Map.of("estado", "FALTA");
        Map<String, String> valorNuevo = Map.of("estado", "JUSTIFICADO", "motivo", "Enfermedad");

        when(objectMapper.writeValueAsString(valorAnterior)).thenReturn("{\"estado\":\"FALTA\"}");
        when(objectMapper.writeValueAsString(valorNuevo)).thenReturn("{\"estado\":\"JUSTIFICADO\",\"motivo\":\"Enfermedad\"}");

        // Act
        auditService.registrar("asistencias", 99L, "JUSTIFICAR_ASISTENCIA", valorAnterior, valorNuevo);

        // Assert
        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());

        Auditoria guardada = captor.getValue();
        assertThat(guardada.getEntidad()).isEqualTo("asistencias");
        assertThat(guardada.getEntidadId()).isEqualTo(99L);
        assertThat(guardada.getAccion()).isEqualTo("JUSTIFICAR_ASISTENCIA");
        assertThat(guardada.getValorAnterior()).isEqualTo("{\"estado\":\"FALTA\"}");
        assertThat(guardada.getValorNuevo()).isEqualTo("{\"estado\":\"JUSTIFICADO\",\"motivo\":\"Enfermedad\"}");
        assertThat(guardada.getUsuario()).isEqualTo(usuarioMock);
    }

    @Test
    void registrar_DebeGuardarSinUsuarioSiNoHayAutenticacion() throws Exception {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        auditService.registrar("estudiantes", 5L, "GENERAR_NUEVO_QR", null, null);

        // Assert
        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());

        Auditoria guardada = captor.getValue();
        assertThat(guardada.getUsuario()).isNull();
        assertThat(guardada.getAccion()).isEqualTo("GENERAR_NUEVO_QR");
        assertThat(guardada.getValorAnterior()).isNull();
        assertThat(guardada.getValorNuevo()).isNull();
    }
}
