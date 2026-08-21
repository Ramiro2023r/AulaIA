package com.aulaia.controller;

import com.aulaia.entity.Curso;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Horario;
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

import java.time.LocalTime;
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
 * Pruebas de integración de la API de horarios (Prompt 5.4): endpoints,
 * autorización (ADMIN crea/modifica; DOCENTE solo sus horarios), filtros,
 * errores y restricción DOCENTE vía MockMvc. Sin PostgreSQL: los
 * repositorios son mocks; JWT/BCrypt/seguridad reales. Datos ficticios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HorarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private HorarioRepository horarioRepository;

    @MockitoBean
    private SesionClaseRepository sesionClaseRepository;

    @MockitoBean
    private AsistenciaRepository asistenciaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.AuditoriaRepository auditoriaRepository;

    @MockitoBean
    private CursoRepository cursoRepository;

    @MockitoBean
    private SeccionRepository seccionRepository;

    @MockitoBean
    private DocenteRepository docenteRepository;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private GradoRepository gradoRepository;

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
        when(docenteRepository.findByUsuarioId(2L))
                .thenReturn(Optional.of(docente(20L, 2L)));

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

    private Docente docente(Long id, Long usuarioId) {
        Docente docente = new Docente();
        docente.setId(id);
        docente.setUsuario(usuario(usuarioId, "doc." + id, Rol.DOCENTE));
        docente.setNombres("Docente");
        docente.setApellidos("De Prueba");
        return docente;
    }

    private Curso curso(Long id, String nombre) {
        Curso curso = new Curso();
        curso.setId(id);
        curso.setNombre(nombre);
        return curso;
    }

    private Seccion seccion(Long id, String nombre) {
        Seccion seccion = new Seccion();
        seccion.setId(id);
        seccion.setNombre(nombre);
        return seccion;
    }

    private Horario horario(Long id, Long docenteId, Long seccionId, Long cursoId) {
        Horario horario = new Horario();
        horario.setId(id);
        horario.setCurso(curso(cursoId, "Matemática"));
        horario.setSeccion(seccion(seccionId, "A"));
        horario.setDocente(docente(docenteId, docenteId));
        horario.setDiaSemana((short) 1);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(9, 0));
        horario.setActivo(true);
        return horario;
    }

    private static String jsonHorario() {
        return """
                {"cursoId": 1, "seccionId": 20, "docenteId": 10,
                 "diaSemana": 1, "horaInicio": "08:00", "horaFin": "09:00",
                 "toleranciaMinutos": 10, "minutosAntesApertura": 15}""";
    }

    private static String jsonHorarioHoraInvalida() {
        return """
                {"cursoId": 1, "seccionId": 20, "docenteId": 10,
                 "diaSemana": 1, "horaInicio": "10:00", "horaFin": "09:00",
                 "toleranciaMinutos": 10, "minutosAntesApertura": 15}""";
    }

    private void stubRelacionesParaCrear() {
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso(1L, "Matemática")));
        when(seccionRepository.findById(20L)).thenReturn(Optional.of(seccion(20L, "A")));
        when(docenteRepository.findById(10L)).thenReturn(Optional.of(docente(10L, 2L)));
        when(horarioRepository.existeConflictoDocente(any(), any(), any(), any(), any())).thenReturn(false);
        when(horarioRepository.existeConflictoSeccion(any(), any(), any(), any(), any())).thenReturn(false);
        when(horarioRepository.saveAndFlush(any(Horario.class))).thenAnswer(inv -> {
            Horario h = inv.getArgument(0);
            h.setId(77L);
            return h;
        });
    }

    // ============ ADMIN ============

    @Test
    void adminListaHorariosDevuelve200() throws Exception {
        when(horarioRepository.buscarConFiltros(null, null, null, null))
                .thenReturn(List.of(horario(1L, 10L, 20L, 1L)));

        mockMvc.perform(get("/api/v1/horarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].curso.nombre").value("Matemática"))
                .andExpect(jsonPath("$[0].docente.id").value(10));
    }

    @Test
    void adminBuscaHorarioPorIdDevuelve200() throws Exception {
        when(horarioRepository.findById(5L)).thenReturn(Optional.of(horario(5L, 10L, 20L, 1L)));

        mockMvc.perform(get("/api/v1/horarios/5").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.seccion.nombre").value("A"));
    }

    @Test
    void adminCreaHorarioDevuelve201() throws Exception {
        stubRelacionesParaCrear();

        mockMvc.perform(post("/api/v1/horarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonHorario()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(77))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.diaSemana").value(1));
    }

    @Test
    void adminActualizaHorarioDevuelve200() throws Exception {
        stubRelacionesParaCrear();
        when(horarioRepository.saveAndFlush(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(horarioRepository.findById(5L)).thenReturn(Optional.of(horario(5L, 10L, 20L, 1L)));

        mockMvc.perform(put("/api/v1/horarios/5").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonHorario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    // ============ DOCENTE ============

    @Test
    void docenteListaSusHorariosDevuelve200() throws Exception {
        when(horarioRepository.buscarConFiltros(20L, null, null, null))
                .thenReturn(List.of(horario(1L, 20L, 20L, 1L)));

        mockMvc.perform(get("/api/v1/horarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].docente.id").value(20));
    }

    @Test
    void docenteNoPuedeEscaparDeSuRestriccionConFiltroDocente() throws Exception {
        when(horarioRepository.buscarConFiltros(20L, null, null, null))
                .thenReturn(List.of(horario(1L, 20L, 20L, 1L)));

        mockMvc.perform(get("/api/v1/horarios?docente=999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].docente.id").value(20));
    }

    @Test
    void docenteVeSuPropioHorarioDevuelve200() throws Exception {
        when(horarioRepository.findById(5L)).thenReturn(Optional.of(horario(5L, 20L, 20L, 1L)));

        mockMvc.perform(get("/api/v1/horarios/5").header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void docenteIntentaVerHorarioAjenoDevuelve403() throws Exception {
        when(horarioRepository.findById(9L)).thenReturn(Optional.of(horario(9L, 21L, 20L, 1L)));

        mockMvc.perform(get("/api/v1/horarios/9").header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void docenteNoPuedeCrearHorarioDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/horarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonHorario()))
                .andExpect(status().isForbidden());
    }

    @Test
    void docenteNoPuedeActualizarHorarioDevuelve403() throws Exception {
        mockMvc.perform(put("/api/v1/horarios/5").header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonHorario()))
                .andExpect(status().isForbidden());
    }

    // ============ Autenticación y errores ============

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/horarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearHorarioConCursoInexistenteDevuelve404() throws Exception {
        stubRelacionesParaCrear();
        when(cursoRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/horarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonHorario()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    @Test
    void crearHorarioConConflictoDocenteDevuelve409() throws Exception {
        stubRelacionesParaCrear();
        when(horarioRepository.existeConflictoDocente(any(), any(), any(), any(), any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/horarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonHorario()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEACHER_SCHEDULE_CONFLICT"));
    }

    @Test
    void crearHorarioConHoraInvalidaDevuelve400() throws Exception {
        stubRelacionesParaCrear();

        mockMvc.perform(post("/api/v1/horarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonHorarioHoraInvalida()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void buscarHorarioInexistenteDevuelve404() throws Exception {
        when(horarioRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/horarios/99").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCHEDULE_NOT_FOUND"));
    }

    @Test
    void listarConDiaFueraDeRangoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/horarios?dia=8")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}