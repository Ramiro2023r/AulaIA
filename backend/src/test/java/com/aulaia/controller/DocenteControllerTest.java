package com.aulaia.controller;

import com.aulaia.entity.Docente;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de la API de docentes (Prompt 5.1): endpoints,
 * autorización (solo ADMIN), errores y no-exposición de credenciales vía
 * MockMvc. Sin PostgreSQL: los repositorios son mocks; JWT/BCrypt/seguridad
 * reales. Datos ficticios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

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

    @MockitoBean
    private GradoRepository gradoRepository;

    @MockitoBean
    private SeccionRepository seccionRepository;

    @MockitoBean
    private CursoRepository cursoRepository;

    @MockitoBean
    private EstudianteRepository estudianteRepository;

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

    private Usuario usuario(Long id, String username, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setPasswordHash("hash-para-test");
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }

    private Docente docente(Long id, Usuario usuario) {
        Docente docente = new Docente();
        docente.setId(id);
        docente.setUsuario(usuario);
        docente.setNombres("Docente");
        docente.setApellidos("De Prueba");
        docente.setActivo(true);
        return docente;
    }

    private static String jsonCrear(String username, String password) {
        return """
                {"username": "%s", "password": "%s", "nombres": "Docente", "apellidos": "De Prueba"}"""
                .formatted(username, password);
    }

    private static String jsonActualizar() {
        return """
                {"nombres": "Docente Actualizado", "apellidos": "Nuevo Apellido"}""";
    }

    @Test
    void getDocentesComoAdminDevuelve200() throws Exception {
        when(docenteRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(docente(1L, usuario(3L, "d.profesor", Rol.DOCENTE))));

        mockMvc.perform(get("/api/v1/docentes").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombres").value("Docente"))
                .andExpect(jsonPath("$[0].usuario.username").value("d.profesor"))
                .andExpect(jsonPath("$[0].usuario.rol").value("DOCENTE"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].usuario.passwordHash").doesNotExist());
    }

    @Test
    void getDocentesComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/docentes").header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getDocentesSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/docentes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getDocentePorIdExistenteDevuelve200() throws Exception {
        when(docenteRepository.findById(1L))
                .thenReturn(Optional.of(docente(1L, usuario(3L, "d.profesor", Rol.DOCENTE))));

        mockMvc.perform(get("/api/v1/docentes/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.usuario.username").value("d.profesor"))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());
    }

    @Test
    void getDocenteInexistenteDevuelve404TeacherNotFound() throws Exception {
        when(docenteRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/docentes/99").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEACHER_NOT_FOUND"));
    }

    @Test
    void postDocenteComoAdminDevuelve201() throws Exception {
        when(usuarioRepository.existsByUsername("d.profesor")).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(3L);
            return u;
        });
        when(docenteRepository.saveAndFlush(any(Docente.class))).thenAnswer(inv -> {
            Docente d = inv.getArgument(0);
            d.setId(10L);
            return d;
        });

        mockMvc.perform(post("/api/v1/docentes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCrear("d.profesor", "clave-ficticia-123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.usuario.username").value("d.profesor"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());
    }

    @Test
    void postDocenteSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/docentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCrear("d.profesor", "clave-ficticia-123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void postDocenteComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/docentes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCrear("d.profesor", "clave-ficticia-123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void postDocenteInvalidoDevuelve400ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/docentes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"\", \"password\": \"\", \"nombres\": \"\", \"apellidos\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void postDocenteConUsernameExistenteDevuelve409() throws Exception {
        when(usuarioRepository.existsByUsername("d.profesor")).thenReturn(true);

        mockMvc.perform(post("/api/v1/docentes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCrear("d.profesor", "clave-ficticia-123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    void putDocenteComoAdminDevuelve200() throws Exception {
        when(docenteRepository.findById(1L))
                .thenReturn(Optional.of(docente(1L, usuario(3L, "d.profesor", Rol.DOCENTE))));
        when(docenteRepository.saveAndFlush(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/v1/docentes/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonActualizar()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Docente Actualizado"))
                .andExpect(jsonPath("$.usuario.username").value("d.profesor"))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());
    }

    @Test
    void putDocenteComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(put("/api/v1/docentes/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonActualizar()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void putDocenteInexistenteDevuelve404() throws Exception {
        when(docenteRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/docentes/99")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonActualizar()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEACHER_NOT_FOUND"));
    }

    @Test
    void patchDesactivarDocenteComoAdminDevuelve200() throws Exception {
        when(docenteRepository.findById(1L))
                .thenReturn(Optional.of(docente(1L, usuario(3L, "d.profesor", Rol.DOCENTE))));
        when(docenteRepository.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(docenteRepository.saveAndFlush(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/v1/docentes/1/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.usuario.activo").value(false));
    }

    @Test
    void patchDesactivarDocenteComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(patch("/api/v1/docentes/1/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}