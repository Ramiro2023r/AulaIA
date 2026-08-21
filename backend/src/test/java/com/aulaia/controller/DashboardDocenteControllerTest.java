package com.aulaia.controller;

import com.aulaia.dto.dashboard.DashboardDocenteResponse;
import com.aulaia.dto.dashboard.EstadisticasAsistencia;
import com.aulaia.dto.sesion.SesionClaseResponse;
import com.aulaia.entity.SesionClaseEstado;
import com.aulaia.security.JwtService;
import com.aulaia.service.DashboardDocenteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardDocenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardDocenteService dashboardDocenteService;

    @MockitoBean
    private com.aulaia.security.JwtService jwtService;

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

    @MockitoBean
    private com.aulaia.repository.AuditoriaRepository auditoriaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    @Test
    @WithMockUser(username = "docente@aulaia.com", roles = {"DOCENTE"})
    void getDashboardDocente_Exito() throws Exception {
        EstadisticasAsistencia stats = new EstadisticasAsistencia(10, 2, 1, 13, 92.3);
        
        SesionClaseResponse clase = new SesionClaseResponse(
                1L, 10L, LocalDate.now(), SesionClaseEstado.ABIERTA, null, null, null, null, new SesionClaseResponse.CursoResumen(1L, "Historia"),
                new SesionClaseResponse.SeccionResumen(2L, "1A"),
                new SesionClaseResponse.DocenteResumen(1L, "Juan", "Perez")
        );

        DashboardDocenteResponse response = new DashboardDocenteResponse(clase, List.of(clase), stats);

        when(dashboardDocenteService.obtenerResumen()).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/docente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadisticas.presentes").value(10))
                .andExpect(jsonPath("$.estadisticas.tardanzas").value(2))
                .andExpect(jsonPath("$.estadisticas.porcentajeAsistencia").value(92.3))
                .andExpect(jsonPath("$.claseActual.id").value(1));
    }
}
