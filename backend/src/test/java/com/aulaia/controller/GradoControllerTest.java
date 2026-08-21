package com.aulaia.controller;

import com.aulaia.dto.grado.GradoRequest;
import com.aulaia.entity.Grado;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.repository.CursoRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.GradoRepository;
import com.aulaia.repository.HorarioRepository;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.SesionClaseRepository;
import com.aulaia.repository.SeccionRepository;
import com.aulaia.repository.UsuarioRepository;
import com.aulaia.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración del módulo Grados (Prompt 3.1): endpoints,
 * autorización y errores vía MockMvc. Sin PostgreSQL: los repositorios
 * son mocks; JWT/BCrypt/seguridad reales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GradoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

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

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    private String adminToken;
    private String docenteToken;

    @BeforeEach
    void setUp() {
        when(usuarioRepository.findByUsername("admin"))
                .thenReturn(Optional.of(usuario(1L, "admin", Rol.ADMIN)));
        when(usuarioRepository.findByUsername("docente"))
                .thenReturn(Optional.of(usuario(2L, "docente", Rol.DOCENTE)));

        adminToken = jwtService.generateToken(usuario(1L, "admin", Rol.ADMIN));
        docenteToken = jwtService.generateToken(usuario(2L, "docente", Rol.DOCENTE));
    }

    @Test
    void getGradosAutenticadoDevuelve200() throws Exception {
        when(gradoRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(grado(1L, "5.º Primaria", "PRIMARIA", 1)));

        mockMvc.perform(get("/api/v1/grados").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("5.º Primaria"))
                .andExpect(jsonPath("$[0].nivel").value("PRIMARIA"))
                .andExpect(jsonPath("$[0].activo").value(true));
    }

    @Test
    void getGradosComoDocenteDevuelve200() throws Exception {
        when(gradoRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/grados").header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isOk());
    }

    @Test
    void getGradoPorIdExistenteDevuelve200() throws Exception {
        when(gradoRepository.findById(1L)).thenReturn(Optional.of(grado(1L, "6.º Primaria", "PRIMARIA", 2)));

        mockMvc.perform(get("/api/v1/grados/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("6.º Primaria"));
    }

    @Test
    void getGradoInexistenteDevuelve404GradeNotFound() throws Exception {
        when(gradoRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/grados/99").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GRADE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Grado no encontrado: 99"));
    }

    @Test
    void postGradoComoAdminDevuelve201() throws Exception {
        when(gradoRepository.save(any(Grado.class))).thenAnswer(inv -> {
            Grado g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });

        mockMvc.perform(post("/api/v1/grados")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"5.º Primaria\",\"nivel\":\"PRIMARIA\",\"orden\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("5.º Primaria"))
                .andExpect(jsonPath("$.nivel").value("PRIMARIA"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void postGradoPermiteMismoNombreQueOtroGrado() throws Exception {
        when(gradoRepository.save(any(Grado.class))).thenAnswer(inv -> {
            Grado g = inv.getArgument(0);
            g.setId(2L);
            return g;
        });

        mockMvc.perform(post("/api/v1/grados")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"5.º Primaria\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nombre").value("5.º Primaria"));
    }

    @Test
    void postGradoComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/grados")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"5.º Primaria\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void postGradoSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/grados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"5.º Primaria\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void postGradoInvalidoDevuelve400ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/grados")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.nombre").exists());
    }

    @Test
    void putGradoComoAdminDevuelve200() throws Exception {
        Grado existente = grado(5L, "Viejo", "PRIMARIA", 1);
        when(gradoRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(gradoRepository.save(any(Grado.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/v1/grados/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"nivel\":\"SECUNDARIA\",\"orden\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nombre").value("Nuevo"))
                .andExpect(jsonPath("$.nivel").value("SECUNDARIA"));
    }

    @Test
    void putGradoComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(put("/api/v1/grados/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void putGradoInexistenteDevuelve404() throws Exception {
        when(gradoRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/grados/99")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GRADE_NOT_FOUND"));
    }

    private Grado grado(Long id, String nombre, String nivel, Integer orden) {
        Grado grado = new Grado();
        grado.setId(id);
        grado.setNombre(nombre);
        grado.setNivel(nivel);
        grado.setOrden(orden);
        return grado;
    }

    private Usuario usuario(Long id, String username, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setPasswordHash("$2a$10$hash-de-prueba-no-real");
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }
}