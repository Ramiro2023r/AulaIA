package com.aulaia.controller;

import com.aulaia.dto.telegram.TelegramVinculacionRequest;
import com.aulaia.entity.EstadoVinculacion;
import com.aulaia.entity.Apoderado;
import com.aulaia.entity.EstudianteApoderado;
import com.aulaia.entity.Parentesco;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Usuario;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.exception.BusinessException;
import com.aulaia.repository.UsuarioRepository;
import com.aulaia.security.JwtService;
import com.aulaia.service.EstudianteService;
import com.aulaia.service.TelegramVinculacionService;
import com.aulaia.dto.TelegramVinculacionResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "telegram.enabled=true",
        "telegram.bot.username=MiBotExitoso",
        "telegram.bot.token=mock-token-123"
})
class EstudianteTelegramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EstudianteService estudianteService;

    @MockitoBean
    private TelegramVinculacionService telegramVinculacionService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

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
    private String docenteToken;

    @BeforeEach
    void setUp() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario(1L, "admin", Rol.ADMIN)));
        when(usuarioRepository.findByUsername("docente")).thenReturn(Optional.of(usuario(2L, "docente", Rol.DOCENTE)));
        adminToken = jwtService.generateToken(usuario(1L, "admin", Rol.ADMIN));
        docenteToken = jwtService.generateToken(usuario(2L, "docente", Rol.DOCENTE));
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
    void apoderadoIdAusenteDevuelveErrorControlado() throws Exception {
        mockMvc.perform(post("/api/v1/estudiantes/1/telegram/vinculacion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TELEGRAM_APODERADO_REQUIRED"));
    }

    @Test
    void creacionCorrectaConApoderado() throws Exception {
        when(telegramVinculacionService.crearVinculacion(eq(1L), eq(10L)))
                .thenReturn(TelegramVinculacionResponseDto.builder()
                        .token("TOKEN_123_APO")
                        .estado(EstadoVinculacion.PENDIENTE)
                        .expiresAt(LocalDateTime.now().plusHours(24))
                        .build());

        TelegramVinculacionRequest req = new TelegramVinculacionRequest();
        req.setApoderadoId(10L);

        mockMvc.perform(post("/api/v1/estudiantes/1/telegram/vinculacion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.telegramUrl").value("https://t.me/MiBotExitoso?start=TOKEN_123_APO")) // 2. deep link correcto
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void estudianteInexistenteDevuelve404() throws Exception {
        when(estudianteService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Estudiante no encontrado"));

        mockMvc.perform(post("/api/v1/estudiantes/99/telegram/vinculacion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound()) // 3. estudiante inexistente
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void apoderadoInexistenteDevuelve400() throws Exception {
        when(telegramVinculacionService.crearVinculacion(eq(1L), eq(99L)))
                .thenThrow(new IllegalArgumentException("Apoderado no encontrado: 99"));

        TelegramVinculacionRequest req = new TelegramVinculacionRequest();
        req.setApoderadoId(99L);

        mockMvc.perform(post("/api/v1/estudiantes/1/telegram/vinculacion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()) // 4. apoderado inexistente
                .andExpect(jsonPath("$.code").value("INVALID_RELATION"));
    }

    @Test
    void apoderadoNoPerteneceAlEstudianteDevuelve400() throws Exception {
        when(telegramVinculacionService.crearVinculacion(eq(1L), eq(2L)))
                .thenThrow(new IllegalArgumentException("El apoderado no está relacionado con el estudiante"));

        TelegramVinculacionRequest req = new TelegramVinculacionRequest();
        req.setApoderadoId(2L);

        mockMvc.perform(post("/api/v1/estudiantes/1/telegram/vinculacion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()) // 5. apoderado no pertenece al estudiante
                .andExpect(jsonPath("$.code").value("INVALID_RELATION"));
    }

    @Test
    void apoderadoInactivoDevuelveErrorControlado() throws Exception {
        when(telegramVinculacionService.crearVinculacion(eq(1L), eq(10L)))
                .thenThrow(new BusinessException("El apoderado seleccionado está inactivo", "TELEGRAM_APODERADO_INACTIVE"));
        TelegramVinculacionRequest req = new TelegramVinculacionRequest();
        req.setApoderadoId(10L);

        mockMvc.perform(post("/api/v1/estudiantes/1/telegram/vinculacion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TELEGRAM_APODERADO_INACTIVE"));
    }

    @Test
    void listaApoderadosAsociadosParaSeleccionarElDestinatario() throws Exception {
        when(estudianteApoderadoRepository.findWithApoderadoByEstudianteId(1L))
                .thenReturn(List.of(relacion(10L, "María", "Pérez", Parentesco.MADRE, true),
                        relacion(11L, "Carlos", "Pérez", Parentesco.PADRE, false)));

        mockMvc.perform(get("/api/v1/estudiantes/1/apoderados")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].parentesco").value("MADRE"))
                .andExpect(jsonPath("$[0].principal").value(true))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].parentesco").value("PADRE"));
    }

    private EstudianteApoderado relacion(Long id, String nombres, String apellidos,
                                         Parentesco parentesco, boolean principal) {
        Apoderado apoderado = new Apoderado();
        apoderado.setId(id);
        apoderado.setNombres(nombres);
        apoderado.setApellidos(apellidos);
        apoderado.setActivo(true);
        EstudianteApoderado relacion = new EstudianteApoderado();
        relacion.setApoderado(apoderado);
        relacion.setParentesco(parentesco);
        relacion.setPrincipal(principal);
        return relacion;
    }
}
