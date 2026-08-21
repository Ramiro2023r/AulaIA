package com.aulaia.controller;

import com.aulaia.entity.Grado;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Seccion;
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
 * Pruebas de integración del módulo Secciones (Prompt 3.2): endpoints,
 * autorización y errores vía MockMvc. Sin PostgreSQL: los repositorios
 * son mocks; JWT/BCrypt/seguridad reales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private SeccionRepository seccionRepository;

    @MockitoBean
    private CursoRepository cursoRepository;

    @MockitoBean
    private EstudianteRepository estudianteRepository;

    @MockitoBean
    private GradoRepository gradoRepository;

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
    void getSeccionesAutenticadoDevuelve200() throws Exception {
        when(seccionRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(seccion(1L, grado(1L, "6.º Primaria"), "A", "2026")));

        mockMvc.perform(get("/api/v1/secciones").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].grado.id").value(1))
                .andExpect(jsonPath("$[0].grado.nombre").value("6.º Primaria"))
                .andExpect(jsonPath("$[0].nombre").value("A"))
                .andExpect(jsonPath("$[0].periodoAcademico").value("2026"))
                .andExpect(jsonPath("$[0].activo").value(true));
    }

    @Test
    void getSeccionesComoDocenteDevuelve200() throws Exception {
        when(seccionRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/secciones").header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isOk());
    }

    @Test
    void getSeccionPorIdExistenteDevuelve200() throws Exception {
        when(seccionRepository.findById(1L))
                .thenReturn(Optional.of(seccion(1L, grado(1L, "6.º Primaria"), "B", "2027")));

        mockMvc.perform(get("/api/v1/secciones/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("B"))
                .andExpect(jsonPath("$.periodoAcademico").value("2027"));
    }

    @Test
    void getSeccionInexistenteDevuelve404SectionNotFound() throws Exception {
        when(seccionRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/secciones/99").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SECTION_NOT_FOUND"));
    }

    @Test
    void postSeccionComoAdminDevuelve201() throws Exception {
        when(gradoRepository.findById(1L)).thenReturn(Optional.of(grado(1L, "6.º Primaria")));
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademico(1L, "A", "2026"))
                .thenReturn(false);
        when(seccionRepository.saveAndFlush(any(Seccion.class))).thenAnswer(inv -> {
            Seccion s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        mockMvc.perform(post("/api/v1/secciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradoId\":1,\"nombre\":\"A\",\"periodoAcademico\":\"2026\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.grado.id").value(1))
                .andExpect(jsonPath("$.nombre").value("A"))
                .andExpect(jsonPath("$.periodoAcademico").value("2026"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void postSeccionComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/secciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradoId\":1,\"nombre\":\"A\",\"periodoAcademico\":\"2026\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void postSeccionSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/secciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradoId\":1,\"nombre\":\"A\",\"periodoAcademico\":\"2026\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void postSeccionInvalidaDevuelve400ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/secciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradoId\":null,\"nombre\":\"\",\"periodoAcademico\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.gradoId").exists())
                .andExpect(jsonPath("$.details.nombre").exists())
                .andExpect(jsonPath("$.details.periodoAcademico").exists());
    }

    @Test
    void postSeccionConGradoInexistenteDevuelve404GradeNotFound() throws Exception {
        when(gradoRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/secciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradoId\":99,\"nombre\":\"A\",\"periodoAcademico\":\"2026\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GRADE_NOT_FOUND"));
    }

    @Test
    void postSeccionDuplicadaDevuelve409() throws Exception {
        when(gradoRepository.findById(1L)).thenReturn(Optional.of(grado(1L, "6.º Primaria")));
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademico(1L, "A", "2026"))
                .thenReturn(true);

        mockMvc.perform(post("/api/v1/secciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradoId\":1,\"nombre\":\"A\",\"periodoAcademico\":\"2026\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SECTION_ALREADY_EXISTS"));
    }

    @Test
    void putSeccionComoAdminDevuelve200() throws Exception {
        when(seccionRepository.findById(5L))
                .thenReturn(Optional.of(seccion(5L, grado(1L, "5.º Primaria"), "A", "2026")));
        when(gradoRepository.findById(2L)).thenReturn(Optional.of(grado(2L, "6.º Primaria")));
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademicoAndIdNot(2L, "B", "2027", 5L))
                .thenReturn(false);
        when(seccionRepository.saveAndFlush(any(Seccion.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/v1/secciones/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradoId\":2,\"nombre\":\"B\",\"periodoAcademico\":\"2027\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.grado.id").value(2))
                .andExpect(jsonPath("$.nombre").value("B"))
                .andExpect(jsonPath("$.periodoAcademico").value("2027"));
    }

    @Test
    void putSeccionComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(put("/api/v1/secciones/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradoId\":1,\"nombre\":\"A\",\"periodoAcademico\":\"2026\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void putSeccionInexistenteDevuelve404() throws Exception {
        when(seccionRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/secciones/99")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradoId\":1,\"nombre\":\"A\",\"periodoAcademico\":\"2026\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SECTION_NOT_FOUND"));
    }

    private Grado grado(Long id, String nombre) {
        Grado grado = new Grado();
        grado.setId(id);
        grado.setNombre(nombre);
        return grado;
    }

    private Seccion seccion(Long id, Grado grado, String nombre, String periodo) {
        Seccion seccion = new Seccion();
        seccion.setId(id);
        seccion.setGrado(grado);
        seccion.setNombre(nombre);
        seccion.setPeriodoAcademico(periodo);
        seccion.setActivo(true);
        return seccion;
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