package com.aulaia.controller;

import com.aulaia.dto.asistencia.RegistrarAsistenciaRequest;
import com.aulaia.dto.asistencia.RegistrarAsistenciaResponse;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.MetodoRegistro;
import com.aulaia.exception.BusinessException;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.security.JwtService;
import com.aulaia.service.AsistenciaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del controlador de Asistencia (Prompt 7.4).
 *
 * <p>Verifica permisos (requiere DOCENTE o ADMIN), mapeo de JSON y traducción
 * de excepciones funcionales por el manejador global.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AsistenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AsistenciaService asistenciaService;

    @MockitoBean
    private com.aulaia.repository.UsuarioRepository usuarioRepository;

    @MockitoBean
    private com.aulaia.repository.GradoRepository gradoRepository;

    @MockitoBean
    private com.aulaia.repository.SeccionRepository seccionRepository;

    @MockitoBean
    private com.aulaia.repository.CursoRepository cursoRepository;

    @MockitoBean
    private com.aulaia.repository.EstudianteRepository estudianteRepository;

    @MockitoBean
    private com.aulaia.repository.DocenteRepository docenteRepository;

    @MockitoBean
    private com.aulaia.repository.HorarioRepository horarioRepository;

    @MockitoBean
    private com.aulaia.repository.SesionClaseRepository sesionClaseRepository;

    @MockitoBean
    private com.aulaia.repository.AsistenciaRepository asistenciaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.AuditoriaRepository auditoriaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    // =========================================================================
    // Helpers
    // =========================================================================

    private RegistrarAsistenciaRequest peticionValida() {
        return new RegistrarAsistenciaRequest("AULAIA:STUDENT:tok", MetodoRegistro.QR, 100L);
    }

    // =========================================================================
    // Seguridad y Validación
    // =========================================================================

    @Test
    void registrar_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/asistencias/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticionValida())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void registrar_conRolIncorrecto_devuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/asistencias/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticionValida())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DOCENTE")
    void registrar_datosInvalidos_devuelve400() throws Exception {
        RegistrarAsistenciaRequest invalida = new RegistrarAsistenciaRequest("", null, null);

        mockMvc.perform(post("/api/v1/asistencias/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalida)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists());
    }

    // =========================================================================
    // Casos Funcionales
    // =========================================================================

    @Test
    @WithMockUser(roles = "DOCENTE")
    void registrar_exito_devuelve200() throws Exception {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        RegistrarAsistenciaResponse resp = new RegistrarAsistenciaResponse(
                true, "Juan", ahora, EstadoAsistencia.PRESENTE, "Asistencia registrada: PRESENTE"
        );

        when(asistenciaService.registrar(any(RegistrarAsistenciaRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/asistencias/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticionValida())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.estado").value("PRESENTE"))
                .andExpect(jsonPath("$.mensaje").value("Asistencia registrada: PRESENTE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void registrar_excepcionNotFound_devuelve404() throws Exception {
        when(asistenciaService.registrar(any()))
                .thenThrow(new ResourceNotFoundException("No se halló la sesión", "SESSION_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/asistencias/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticionValida())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "DOCENTE")
    void registrar_excepcionNegocio_devuelve422() throws Exception {
        when(asistenciaService.registrar(any()))
                .thenThrow(new BusinessException("Sesión cerrada", "SESSION_NOT_ACTIVE"));

        mockMvc.perform(post("/api/v1/asistencias/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticionValida())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "DOCENTE")
    void registrar_excepcionConflicto_devuelve409() throws Exception {
        when(asistenciaService.registrar(any()))
                .thenThrow(new ConflictException("Ya registrada", "ATTENDANCE_ALREADY_REGISTERED"));

        mockMvc.perform(post("/api/v1/asistencias/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticionValida())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTENDANCE_ALREADY_REGISTERED"));
    }

    // =========================================================================
    // Corrección Manual
    // =========================================================================

    @Test
    void correccion_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/asistencias/1/correccion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void correccion_conRolIncorrecto_devuelve403() throws Exception {
        com.aulaia.dto.asistencia.AsistenciaCorreccionRequest req = new com.aulaia.dto.asistencia.AsistenciaCorreccionRequest(EstadoAsistencia.JUSTIFICADO, "Motivo");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/asistencias/1/correccion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DOCENTE")
    void correccion_exito_devuelve200() throws Exception {
        com.aulaia.dto.asistencia.AsistenciaCorreccionRequest req = new com.aulaia.dto.asistencia.AsistenciaCorreccionRequest(EstadoAsistencia.JUSTIFICADO, "Motivo de prueba");
        
        com.aulaia.dto.asistencia.AsistenciaResponse resp = new com.aulaia.dto.asistencia.AsistenciaResponse(
            1L, 100L, 20L, "Juan", "Perez", OffsetDateTime.now(ZoneOffset.UTC), EstadoAsistencia.JUSTIFICADO, MetodoRegistro.MANUAL_DOCENTE, "Motivo de prueba"
        );

        when(asistenciaService.correccionManual(any(Long.class), any(com.aulaia.dto.asistencia.AsistenciaCorreccionRequest.class))).thenReturn(resp);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/asistencias/1/correccion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("JUSTIFICADO"));
    }
}
