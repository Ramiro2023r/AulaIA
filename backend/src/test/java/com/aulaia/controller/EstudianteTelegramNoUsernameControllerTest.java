package com.aulaia.controller;

import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.repository.UsuarioRepository;
import com.aulaia.security.JwtService;
import com.aulaia.service.EstudianteService;
import com.aulaia.service.TelegramVinculacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "telegram.enabled=true",
        "telegram.bot.username=",
        "telegram.bot.token=mock-token-123"
})
class EstudianteTelegramNoUsernameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private EstudianteService estudianteService;

    @MockitoBean
    private TelegramVinculacionService telegramVinculacionService;

    @MockitoBean private com.aulaia.repository.GradoRepository gradoRepository;
    @MockitoBean private com.aulaia.repository.SeccionRepository seccionRepository;
    @MockitoBean private com.aulaia.repository.CursoRepository cursoRepository;
    @MockitoBean private com.aulaia.repository.EstudianteRepository estudianteRepository;
    @MockitoBean private com.aulaia.repository.DocenteRepository docenteRepository;
    @MockitoBean private com.aulaia.repository.HorarioRepository horarioRepository;
    @MockitoBean private com.aulaia.repository.SesionClaseRepository sesionClaseRepository;
    @MockitoBean private com.aulaia.repository.AsistenciaRepository asistenciaRepository;
    @MockitoBean private com.aulaia.repository.EstudianteApoderadoRepository estudianteApoderadoRepository;
    @MockitoBean private com.aulaia.repository.AuditoriaRepository auditoriaRepository;
    @MockitoBean private com.aulaia.repository.JustificacionRepository justificacionRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario(1L, "admin", Rol.ADMIN)));
        adminToken = jwtService.generateToken(usuario(1L, "admin", Rol.ADMIN));
    }

    private Usuario usuario(Long id, String username, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setPasswordHash("$2a$10$hash");
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }

    @Test
    void botUsernameVacioDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/estudiantes/1/telegram/vinculacion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest()) // 7. bot username vacío
                .andExpect(jsonPath("$.code").value("TELEGRAM_NOT_CONFIGURED"));
    }
}
