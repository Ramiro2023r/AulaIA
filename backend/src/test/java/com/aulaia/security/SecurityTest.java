package com.aulaia.security;

import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.CursoRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.GradoRepository;
import com.aulaia.repository.HorarioRepository;
import com.aulaia.repository.SesionClaseRepository;
import com.aulaia.repository.SeccionRepository;
import com.aulaia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad (Prompts 2.2 y 2.4): rutas públicas, 401/403 JSON,
 * autenticación Bearer JWT end-to-end (filtro + JwtService +
 * CustomUserDetailsService) y autorización por roles.
 *
 * <p>No dependen de PostgreSQL: {@code UsuarioRepository} es un mock
 * (Testcontainers llegará en Sprint 18).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

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

    @MockitoBean
    private com.aulaia.repository.AuditoriaRepository auditoriaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    @BeforeEach
    void setUpUsuarios() {
        when(usuarioRepository.findByUsername("admin"))
                .thenReturn(Optional.of(usuario(1L, "admin", Rol.ADMIN, true)));
        when(usuarioRepository.findByUsername("docente"))
                .thenReturn(Optional.of(usuario(2L, "docente", Rol.DOCENTE, true)));
        when(usuarioRepository.findByUsername("inactivo"))
                .thenReturn(Optional.of(usuario(3L, "inactivo", Rol.DOCENTE, false)));
        when(usuarioRepository.findByUsername("no-existe"))
                .thenReturn(Optional.empty());
    }

    @Test
    void actuatorHealthEsPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void apiDocsEsPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiEsAccesible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void rutaProtegidaSinAutenticacionDevuelve401Json() throws Exception {
        mockMvc.perform(get("/test/security/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Autenticación requerida"))
                .andExpect(jsonPath("$.path").value("/test/security/admin"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("java."))));
    }

    @Test
    void accesoAutenticadoSinRolSuficienteDevuelve403Json() throws Exception {
        mockMvc.perform(get("/test/security/admin")
                        .with(user("docente").roles("DOCENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("No tienes permiso para realizar esta acción"))
                .andExpect(jsonPath("$.path").value("/test/security/admin"))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void bearerValidoAdminAutenticaConRolAdmin() throws Exception {
        String token = jwtService.generateToken(usuario(1L, "admin", Rol.ADMIN, true));

        mockMvc.perform(get("/test/security/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test/security/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin")))
                .andExpect(content().string(containsString("ROLE_ADMIN")));
    }

    @Test
    void bearerValidoDocenteAutenticaConRolDocente() throws Exception {
        String token = jwtService.generateToken(usuario(2L, "docente", Rol.DOCENTE, true));

        mockMvc.perform(get("/test/security/docente")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test/security/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("docente")))
                .andExpect(content().string(containsString("ROLE_DOCENTE")));
    }

    @Test
    void tokenExpiradoDevuelve401() throws Exception {
        String token = jwtService.generateToken(usuario(1L, "admin", Rol.ADMIN, true),
                Duration.ofSeconds(-1));

        mockMvc.perform(get("/test/security/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
    }

    @Test
    void firmaInvalidaDevuelve401() throws Exception {
        JwtService otroServicio = new JwtService(
                "otro-secreto-diferente-para-firma-invalida-1234567890", 3600000);
        String token = otroServicio.generateToken(usuario(1L, "admin", Rol.ADMIN, true));

        mockMvc.perform(get("/test/security/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Token inválido o expirado"))
                .andExpect(content().string(not(containsString("signature"))));
    }

    @Test
    void tokenMalformadoDevuelve401() throws Exception {
        mockMvc.perform(get("/test/security/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer abc.xyz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
    }

    @Test
    void usuarioInexistenteDevuelve401() throws Exception {
        String token = jwtService.generateToken(usuario(99L, "no-existe", Rol.DOCENTE, true));

        mockMvc.perform(get("/test/security/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
    }

    @Test
    void usuarioInactivoDevuelve401() throws Exception {
        String token = jwtService.generateToken(usuario(3L, "inactivo", Rol.DOCENTE, false));

        mockMvc.perform(get("/test/security/docente")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Token inválido o expirado"));
    }

    @Test
    void docenteConTokenEnRutaAdminDevuelve403() throws Exception {
        String token = jwtService.generateToken(usuario(2L, "docente", Rol.DOCENTE, true));

        mockMvc.perform(get("/test/security/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void headerSinPrefijoBearerNoAutentica() throws Exception {
        mockMvc.perform(get("/test/security/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Basic abc123"))
                .andExpect(status().isUnauthorized());
    }

    private Usuario usuario(Long id, String username, Rol rol, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setPasswordHash("$2a$10$hash-de-prueba-no-real");
        usuario.setRol(rol);
        usuario.setActivo(activo);
        return usuario;
    }
}