package com.aulaia.controller;

import com.aulaia.entity.Curso;
import com.aulaia.entity.Docente;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.Grado;
import com.aulaia.entity.Horario;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.SesionClase;
import com.aulaia.entity.SesionClaseEstado;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de la API de sesiones (Prompts 6.3 y 6.4):
 * POST /api/v1/sesiones/{id}/abrir y los listados GET /api/v1/sesiones,
 * GET /api/v1/sesiones/activas y GET /api/v1/sesiones/{id} — autorización
 * (ADMIN / DOCENTE propietario / DOCENTE ajeno), 401 sin token, 400 de
 * fecha/estado inválidos, 404 sesión inexistente, 409 estado inválido y
 * contrato de respuesta. Sin PostgreSQL: repositorios mockeados;
 * JWT/BCrypt/seguridad reales. Datos ficticios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SesionClaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private SesionClaseRepository sesionClaseRepository;

    @MockitoBean
    private AsistenciaRepository asistenciaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.AuditoriaRepository auditoriaRepository;

    @MockitoBean
    private HorarioRepository horarioRepository;

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
    private String docenteAToken;
    private String docenteBToken;

    private SesionClase sesionDelDocenteB;

    @BeforeEach
    void setUp() {
        Usuario admin = usuario(1L, "admin", Rol.ADMIN);
        Usuario docenteA = usuario(2L, "doc.a", Rol.DOCENTE);
        Usuario docenteB = usuario(3L, "doc.b", Rol.DOCENTE);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByUsername("doc.a")).thenReturn(Optional.of(docenteA));
        when(usuarioRepository.findByUsername("doc.b")).thenReturn(Optional.of(docenteB));
        when(docenteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(docente(20L, 2L)));
        when(docenteRepository.findByUsuarioId(3L)).thenReturn(Optional.of(docente(30L, 3L)));

        adminToken = jwtService.generateToken(admin);
        docenteAToken = jwtService.generateToken(docenteA);
        docenteBToken = jwtService.generateToken(docenteB);

        Horario horarioB = horario(7L, 30L);
        sesionDelDocenteB = sesion(100L, horarioB, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(sesionDelDocenteB));
        when(sesionClaseRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
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

    private Horario horario(Long id, Long docenteId) {
        Horario horario = new Horario();
        horario.setId(id);
        horario.setCurso(new Curso());
        horario.setSeccion(new Seccion());
        horario.setDocente(docente(docenteId, docenteId));
        horario.setDiaSemana((short) 1);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(9, 0));
        horario.setActivo(true);
        return horario;
    }

    private SesionClase sesion(Long id, Horario horario, LocalDate fecha) {
        SesionClase sesion = new SesionClase();
        sesion.setId(id);
        sesion.setHorario(horario);
        sesion.setFecha(fecha);
        sesion.setEstado(SesionClaseEstado.PROGRAMADA);
        return sesion;
    }

    @Test
    void adminAbreSesionDevuelve200ConEstadoAbiertaYHora() throws Exception {
        mockMvc.perform(post("/api/v1/sesiones/100/abrir")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.horaApertura").isNotEmpty())
                .andExpect(jsonPath("$.horaCierre").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void adminCierraSesionDevuelve200ConEstadoCerradaYHora() throws Exception {
        SesionClase s = sesion(100L, horario(10L, 20L), LocalDate.now());
        s.setEstado(SesionClaseEstado.ABIERTA);
        s.setHoraApertura(java.time.OffsetDateTime.now());
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(s));

        mockMvc.perform(post("/api/v1/sesiones/100/cerrar")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.horaCierre").isNotEmpty());
    }

    @Test
    void docentePropietarioAbreSesionDevuelve200() throws Exception {
        when(sesionClaseRepository.findById(100L))
                .thenReturn(Optional.of(sesion(100L, horario(8L, 20L), LocalDate.of(2026, 8, 19))));

        mockMvc.perform(post("/api/v1/sesiones/100/abrir")
                        .header("Authorization", "Bearer " + docenteAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.horaApertura").isNotEmpty());
    }

    @Test
    void docenteAjenoRecibe403() throws Exception {
        mockMvc.perform(post("/api/v1/sesiones/100/abrir")
                        .header("Authorization", "Bearer " + docenteAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mockMvc.perform(post("/api/v1/sesiones/100/abrir"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sesionInexistenteRecibe404() throws Exception {
        when(sesionClaseRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/sesiones/999/abrir")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void estadoInvalidoRecibe409() throws Exception {
        sesionDelDocenteB.setEstado(SesionClaseEstado.CANCELADA);

        mockMvc.perform(post("/api/v1/sesiones/100/abrir")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_INVALID_STATE"));
    }

    @Test
    void abiertaEsIdempotenteYConservaHoraApertura() throws Exception {
        sesionDelDocenteB.setEstado(SesionClaseEstado.ABIERTA);
        sesionDelDocenteB.setHoraApertura(java.time.OffsetDateTime.parse("2026-08-19T08:05:00-05:00"));

        mockMvc.perform(post("/api/v1/sesiones/100/abrir")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.horaApertura").value("2026-08-19T08:05:00-05:00"));
    }

    @Test
    void docentePropietarioPuedeAbrirSuPropiaSesionAunqueNoSeaAdmin() throws Exception {
        when(sesionClaseRepository.findById(100L))
                .thenReturn(Optional.of(sesion(100L, horario(9L, 20L), LocalDate.of(2026, 8, 19))));

        mockMvc.perform(post("/api/v1/sesiones/100/abrir")
                        .header("Authorization", "Bearer " + docenteAToken))
                .andExpect(status().isOk());
    }

    @Test
    void docentePropietarioPuedeCerrarSuPropiaSesion() throws Exception {
        SesionClase s = sesion(100L, horario(9L, 20L), LocalDate.of(2026, 8, 19));
        s.setEstado(SesionClaseEstado.ABIERTA);
        when(sesionClaseRepository.findById(100L))
                .thenReturn(Optional.of(s));

        mockMvc.perform(post("/api/v1/sesiones/100/cerrar")
                        .header("Authorization", "Bearer " + docenteAToken))
                .andExpect(status().isOk());
    }

    @Test
    void cerrarSesionDocenteAjenoRecibe403() throws Exception {
        SesionClase s = sesion(100L, horario(9L, 21L), LocalDate.of(2026, 8, 19));
        s.setEstado(SesionClaseEstado.ABIERTA);
        when(sesionClaseRepository.findById(100L)).thenReturn(Optional.of(s));

        mockMvc.perform(post("/api/v1/sesiones/100/cerrar")
                        .header("Authorization", "Bearer " + docenteAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ===================== Prompt 6.4 — listados =====================

    @Test
    void adminListaSesionesDevuelve200ConListaYResumenes() throws Exception {
        Curso curso = new Curso();
        curso.setId(3L);
        curso.setNombre("Matemática");
        Horario horarioB = horario(7L, 30L);
        horarioB.setCurso(curso);
        SesionClase sesion = sesion(100L, horarioB, LocalDate.of(2026, 8, 19));
        when(sesionClaseRepository.buscarConFiltros(null, null, null, null, null))
                .thenReturn(List.of(sesion));

        mockMvc.perform(get("/api/v1/sesiones")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].horarioId").value(7))
                .andExpect(jsonPath("$[0].fecha").value("2026-08-19"))
                .andExpect(jsonPath("$[0].estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$[0].curso.nombre").value("Matemática"));
    }

    @Test
    void adminListaConTodosLosFiltrosLosPropagaAlRepositorio() throws Exception {
        when(sesionClaseRepository.buscarConFiltros(LocalDate.of(2026, 8, 19), 30L, 7L, 3L,
                SesionClaseEstado.CERRADA)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/sesiones")
                        .queryParam("fecha", "2026-08-19")
                        .queryParam("docente", "30")
                        .queryParam("seccion", "7")
                        .queryParam("curso", "3")
                        .queryParam("estado", "CERRADA")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(sesionClaseRepository).buscarConFiltros(LocalDate.of(2026, 8, 19), 30L, 7L, 3L,
                SesionClaseEstado.CERRADA);
    }

    @Test
    void docenteListaConDocenteIdAjenoSeFuerzaSuPropioDocente() throws Exception {
        when(sesionClaseRepository.buscarConFiltros(null, 20L, null, null, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/sesiones")
                        .queryParam("docente", "30")
                        .header("Authorization", "Bearer " + docenteAToken))
                .andExpect(status().isOk());

        verify(sesionClaseRepository).buscarConFiltros(null, 20L, null, null, null);
    }

    @Test
    void adminListaActivasDevuelve200() throws Exception {
        when(sesionClaseRepository.buscarConFiltros(null, null, null, null,
                SesionClaseEstado.ABIERTA)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/sesiones/activas")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(sesionClaseRepository).buscarConFiltros(null, null, null, null,
                SesionClaseEstado.ABIERTA);
    }

    @Test
    void docenteListaActivasSoloDeSusHorarios() throws Exception {
        when(sesionClaseRepository.buscarConFiltros(null, 20L, null, null,
                SesionClaseEstado.ABIERTA)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/sesiones/activas")
                        .header("Authorization", "Bearer " + docenteAToken))
                .andExpect(status().isOk());

        verify(sesionClaseRepository).buscarConFiltros(null, 20L, null, null,
                SesionClaseEstado.ABIERTA);
    }

    @Test
    void adminBuscaSesionPorIdDevuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/sesiones/100")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.horarioId").value(7))
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));
    }

    @Test
    void docenteBuscaSuPropiaSesionPorIdDevuelve200() throws Exception {
        when(sesionClaseRepository.findById(100L))
                .thenReturn(Optional.of(sesion(100L, horario(8L, 20L), LocalDate.of(2026, 8, 19))));

        mockMvc.perform(get("/api/v1/sesiones/100")
                        .header("Authorization", "Bearer " + docenteAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarioId").value(8));
    }

    @Test
    void docenteBuscaSesionAjenaRecibe403() throws Exception {
        mockMvc.perform(get("/api/v1/sesiones/100")
                        .header("Authorization", "Bearer " + docenteAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void sesionInexistenteEnListadoRecibe404() throws Exception {
        when(sesionClaseRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/sesiones/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void listarSesionesSinTokenRecibe401() throws Exception {
        mockMvc.perform(get("/api/v1/sesiones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarConEstadoInvalidoRecibe400() throws Exception {
        mockMvc.perform(get("/api/v1/sesiones")
                        .queryParam("estado", "INVALIDO")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listarConFechaInvalidaRecibe400() throws Exception {
        mockMvc.perform(get("/api/v1/sesiones")
                        .queryParam("fecha", "2026-99-99")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listarActivasSinTokenRecibe401() throws Exception {
        mockMvc.perform(get("/api/v1/sesiones/activas"))
                .andExpect(status().isUnauthorized());
    }
}