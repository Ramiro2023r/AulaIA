package com.aulaia.exception;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del manejo global de errores (Prompt 1.4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorHandlingTest {

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
    void resourceNotFoundExceptionDevuelve404() throws Exception {
        mockMvc.perform(get("/test/error/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.path").value("/test/error/resource-not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void businessExceptionDevuelve400() throws Exception {
        mockMvc.perform(get("/test/error/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Regla de negocio violada"))
                .andExpect(jsonPath("$.path").value("/test/error/business"));
    }

    @Test
    void conflictExceptionDevuelve409() throws Exception {
        mockMvc.perform(get("/test/error/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Conflicto de datos"))
                .andExpect(jsonPath("$.path").value("/test/error/conflict"));
    }

    @Test
    void forbiddenExceptionDevuelve403() throws Exception {
        mockMvc.perform(get("/test/error/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/test/error/forbidden"));
    }

    @Test
    void beanValidationDevuelve400ConDetalles() throws Exception {
        mockMvc.perform(post("/test/error/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Los datos enviados no son válidos"))
                .andExpect(jsonPath("$.details.username").exists())
                .andExpect(jsonPath("$.details.nombre").exists())
                .andExpect(jsonPath("$.path").value("/test/error/validation"));
    }

    @Test
    void jsonInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/test/error/payload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_JSON"))
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud no es válido"));
    }

    @Test
    void errorInesperadoDevuelve500SinStackTrace() throws Exception {
        mockMvc.perform(get("/test/error/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno"))
                .andExpect(jsonPath("$.path").value("/test/error/unexpected"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("java."))));
    }

    @Test
    void rutaProtegidaSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Autenticación requerida"));
    }
}