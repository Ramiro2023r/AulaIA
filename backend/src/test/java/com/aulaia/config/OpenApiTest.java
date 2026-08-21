package com.aulaia.config;

import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.CursoRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.GradoRepository;
import com.aulaia.repository.HorarioRepository;
import com.aulaia.repository.SesionClaseRepository;
import com.aulaia.repository.SeccionRepository;
import com.aulaia.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la configuración OpenAPI 3 + Swagger UI (Prompt 1.5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private GradoRepository gradoRepository;

    @MockitoBean
    private SeccionRepository seccionRepository;

    @MockitoBean
    private CursoRepository cursoRepository;

    @MockitoBean
    private EstudianteRepository estudianteRepository;

    @MockitoBean
    private DocenteRepository docenteRepository;

    @MockitoBean
    private HorarioRepository horarioRepository;

    @MockitoBean
    private SesionClaseRepository sesionClaseRepository;

    @MockitoBean
    private AsistenciaRepository asistenciaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.AuditoriaRepository auditoriaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    @Test
    void apiDocsDevuelveOpenApi3ConInfoDeAulaia() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("AulaIA API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.info.description", startsWith("API REST del Sistema Inteligente de Asistencia Escolar AulaIA")));
    }

    @Test
    void apiDocsDeclaraEsquemaDeSeguridadBearerJwt() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void swaggerUiCarga() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void actuatorHealthSigueAccesible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void documentacionNoRompioElManejoGlobalDeErrores() throws Exception {
        mockMvc.perform(get("/test/error/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void apiDocsDocumentaElTagHorariosConLosEndpointsReales() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths./api/v1/horarios.get.tags[0]").value("Horarios"))
                .andExpect(jsonPath("$.paths./api/v1/horarios.post.tags[0]").value("Horarios"))
                .andExpect(jsonPath("$.paths./api/v1/horarios/{id}.get.tags[0]").value("Horarios"))
                .andExpect(jsonPath("$.paths./api/v1/horarios/{id}.put.tags[0]").value("Horarios"))
                .andExpect(jsonPath("$.paths./api/v1/horarios/{id}.delete").doesNotExist());
    }
}