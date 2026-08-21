package com.aulaia.controller;

import com.aulaia.entity.Curso;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración del módulo Cursos (Prompt 3.3): endpoints,
 * autorización y errores vía MockMvc. Sin PostgreSQL: los repositorios
 * son mocks; JWT/BCrypt/seguridad reales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CursoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CursoRepository cursoRepository;

    @MockitoBean
    private EstudianteRepository estudianteRepository;

    @MockitoBean
    private GradoRepository gradoRepository;

    @MockitoBean
    private SeccionRepository seccionRepository;

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
    void getCursosAutenticadoDevuelve200() throws Exception {
        when(cursoRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(curso(1L, "Computación", "Curso de ofimática")));

        mockMvc.perform(get("/api/v1/cursos").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Computación"))
                .andExpect(jsonPath("$[0].descripcion").value("Curso de ofimática"))
                .andExpect(jsonPath("$[0].activo").value(true));
    }

    @Test
    void getCursosComoDocenteDevuelve200() throws Exception {
        when(cursoRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cursos").header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isOk());
    }

    @Test
    void getCursoPorIdExistenteDevuelve200() throws Exception {
        when(cursoRepository.findById(1L))
                .thenReturn(Optional.of(curso(1L, "Computación", "Curso de ofimática")));

        mockMvc.perform(get("/api/v1/cursos/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Computación"))
                .andExpect(jsonPath("$.descripcion").value("Curso de ofimática"));
    }

    @Test
    void getCursoInexistenteDevuelve404CourseNotFound() throws Exception {
        when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/cursos/99").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    @Test
    void postCursoComoAdminDevuelve201() throws Exception {
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> {
            Curso c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        mockMvc.perform(post("/api/v1/cursos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Computación\",\"descripcion\":\"Curso de ofimática\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Computación"))
                .andExpect(jsonPath("$.descripcion").value("Curso de ofimática"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void postCursoComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/cursos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Computación\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void postCursoSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Computación\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void postCursoInvalidoDevuelve400ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/cursos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.nombre").exists());
    }

    @Test
    void putCursoComoAdminDevuelve200() throws Exception {
        when(cursoRepository.findById(5L))
                .thenReturn(Optional.of(curso(5L, "Computación", "Descripción anterior")));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/v1/cursos/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Matemáticas\",\"descripcion\":\"Álgebra\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nombre").value("Matemáticas"))
                .andExpect(jsonPath("$.descripcion").value("Álgebra"));
    }

    @Test
    void putCursoComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(put("/api/v1/cursos/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Matemáticas\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void putCursoInexistenteDevuelve404() throws Exception {
        when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/cursos/99")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Matemáticas\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    private Curso curso(Long id, String nombre, String descripcion) {
        Curso curso = new Curso();
        curso.setId(id);
        curso.setNombre(nombre);
        curso.setDescripcion(descripcion);
        curso.setActivo(true);
        return curso;
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