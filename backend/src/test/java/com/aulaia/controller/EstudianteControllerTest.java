package com.aulaia.controller;

import com.aulaia.entity.Estudiante;
import com.aulaia.entity.Apoderado;
import com.aulaia.entity.EstudianteApoderado;
import com.aulaia.entity.Rol;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.Usuario;
import com.aulaia.repository.CursoRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.EstudianteApoderadoRepository;
import com.aulaia.repository.ApoderadoRepository;
import com.aulaia.repository.GradoRepository;
import com.aulaia.repository.HorarioRepository;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.SesionClaseRepository;
import com.aulaia.repository.SeccionRepository;
import com.aulaia.repository.UsuarioRepository;
import com.aulaia.security.JwtService;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de la API de estudiantes (Prompt 4.3): endpoints,
 * autorización, filtros y errores vía MockMvc. Sin PostgreSQL: los
 * repositorios son mocks; JWT/BCrypt/seguridad reales. Datos ficticios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstudianteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private EstudianteRepository estudianteRepository;

    @MockitoBean
    private SeccionRepository seccionRepository;

    @MockitoBean
    private GradoRepository gradoRepository;

    @MockitoBean
    private CursoRepository cursoRepository;

    @MockitoBean
    private DocenteRepository docenteRepository;

    @MockitoBean
    private HorarioRepository horarioRepository;

    @MockitoBean
    private SesionClaseRepository sesionClaseRepository;

    @MockitoBean
    private AsistenciaRepository asistenciaRepository;

    @MockitoBean
    private EstudianteApoderadoRepository estudianteApoderadoRepository;

    @MockitoBean
    private ApoderadoRepository apoderadoRepository;

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
    void getEstudiantesComoAdminDevuelve200() throws Exception {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(estudiante(1L, "COD001", seccion(1L, "A"))));

        mockMvc.perform(get("/api/v1/estudiantes").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].codigo").value("COD001"))
                .andExpect(jsonPath("$[0].seccion.id").value(1))
                .andExpect(jsonPath("$[0].seccion.nombre").value("A"))
                .andExpect(jsonPath("$[0].activo").value(true))
                .andExpect(jsonPath("$[0].qrToken").doesNotExist());
    }

    @Test
    void getEstudiantesComoDocenteDevuelve200() throws Exception {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/estudiantes").header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isOk());
    }

    @Test
    void getEstudiantesSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void crearApoderadoComoAdminLoRegistraYAsociaAlEstudiante() throws Exception {
        Estudiante estudiante = estudiante(1L, "COD001", seccion(1L, "A"));
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteApoderadoRepository.findByEstudianteId(1L)).thenReturn(List.of());
        when(apoderadoRepository.save(any(Apoderado.class))).thenAnswer(invocation -> {
            Apoderado apoderado = invocation.getArgument(0);
            apoderado.setId(9L);
            return apoderado;
        });
        when(estudianteApoderadoRepository.save(any(EstudianteApoderado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/estudiantes/1/apoderados")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombres":"María","apellidos":"Pérez","telefono":"999111222","parentesco":"MADRE","principal":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.nombres").value("María"))
                .andExpect(jsonPath("$.parentesco").value("MADRE"))
                .andExpect(jsonPath("$.principal").value(true))
                .andExpect(jsonPath("$.activo").value(true));

        verify(estudianteApoderadoRepository).save(any(EstudianteApoderado.class));
    }

    @Test
    void buscarApoderadosDisponiblesComoAdminDevuelveSoloResultadosNoAsociados() throws Exception {
        Estudiante estudiante = estudiante(1L, "COD001", seccion(1L, "A"));
        Apoderado apoderado = new Apoderado();
        apoderado.setId(9L);
        apoderado.setNombres("María");
        apoderado.setApellidos("Pérez");
        apoderado.setTelefono("999111222");
        apoderado.setActivo(true);
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(apoderadoRepository.buscarActivosNoAsociados(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("María"), any(Pageable.class))).thenReturn(List.of(apoderado));

        mockMvc.perform(get("/api/v1/estudiantes/1/apoderados/disponibles")
                        .param("buscar", "María")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].nombres").value("María"))
                .andExpect(jsonPath("$[0].telefono").value("999111222"));
    }

    @Test
    void asociarApoderadoExistenteComoAdminNoLoDuplica() throws Exception {
        Estudiante estudiante = estudiante(1L, "COD001", seccion(1L, "A"));
        Apoderado apoderado = new Apoderado();
        apoderado.setId(9L);
        apoderado.setNombres("María");
        apoderado.setApellidos("Pérez");
        apoderado.setActivo(true);
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(apoderadoRepository.findById(9L)).thenReturn(Optional.of(apoderado));
        when(estudianteApoderadoRepository.existsByEstudianteIdAndApoderadoId(1L, 9L)).thenReturn(false);
        when(estudianteApoderadoRepository.save(any(EstudianteApoderado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/estudiantes/1/apoderados/9")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentesco\":\"MADRE\",\"principal\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.parentesco").value("MADRE"))
                .andExpect(jsonPath("$.principal").value(true));

        verify(apoderadoRepository, org.mockito.Mockito.never()).save(any(Apoderado.class));
        verify(estudianteApoderadoRepository).save(any(EstudianteApoderado.class));
    }

    @Test
    void asociarApoderadoYaRelacionadoDevuelveConflicto() throws Exception {
        Estudiante estudiante = estudiante(1L, "COD001", seccion(1L, "A"));
        Apoderado apoderado = new Apoderado();
        apoderado.setId(9L);
        apoderado.setActivo(true);
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(apoderadoRepository.findById(9L)).thenReturn(Optional.of(apoderado));
        when(estudianteApoderadoRepository.existsByEstudianteIdAndApoderadoId(1L, 9L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/estudiantes/1/apoderados/9")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentesco\":\"MADRE\",\"principal\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARENT_ALREADY_ASSOCIATED"));
    }

    @Test
    void getEstudiantePorIdExistenteDevuelve200() throws Exception {
        when(estudianteRepository.findById(1L))
                .thenReturn(Optional.of(estudiante(1L, "COD001", seccion(1L, "A"))));

        mockMvc.perform(get("/api/v1/estudiantes/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codigo").value("COD001"));
    }

    @Test
    void getEstudianteInexistenteDevuelve404StudentNotFound() throws Exception {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/estudiantes/99").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void postEstudianteComoAdminDevuelve201() throws Exception {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo(anyString())).thenReturn(false);
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> {
            Estudiante e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        mockMvc.perform(post("/api/v1/estudiantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"COD001\",\"nombres\":\"Estudiante\",\"apellidos\":\"De Prueba\",\"seccionId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codigo").value("COD001"))
                .andExpect(jsonPath("$.seccion.id").value(1))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.qrToken").doesNotExist());
    }

    @Test
    void postEstudianteComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/estudiantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"COD001\",\"nombres\":\"Estudiante\",\"apellidos\":\"De Prueba\",\"seccionId\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void postEstudianteSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"COD001\",\"nombres\":\"Estudiante\",\"apellidos\":\"De Prueba\",\"seccionId\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void postEstudianteInvalidoDevuelve400ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/estudiantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"nombres\":\"\",\"apellidos\":\"\",\"seccionId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.codigo").exists())
                .andExpect(jsonPath("$.details.nombres").exists())
                .andExpect(jsonPath("$.details.apellidos").exists())
                .andExpect(jsonPath("$.details.seccionId").exists());
    }

    @Test
    void postEstudianteConSeccionInexistenteDevuelve404SectionNotFound() throws Exception {
        when(seccionRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/estudiantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"COD001\",\"nombres\":\"Estudiante\",\"apellidos\":\"De Prueba\",\"seccionId\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SECTION_NOT_FOUND"));
    }

    @Test
    void postEstudianteConCodigoDuplicadoDevuelve409() throws Exception {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo("COD001")).thenReturn(true);

        mockMvc.perform(post("/api/v1/estudiantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"COD001\",\"nombres\":\"Estudiante\",\"apellidos\":\"De Prueba\",\"seccionId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDENT_CODE_ALREADY_EXISTS"));
    }

    @Test
    void putEstudianteComoAdminDevuelve200() throws Exception {
        when(estudianteRepository.findById(5L))
                .thenReturn(Optional.of(estudiante(5L, "COD001", seccion(1L, "A"))));
        when(seccionRepository.findById(2L)).thenReturn(Optional.of(seccion(2L, "B")));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/v1/estudiantes/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"COD009\",\"nombres\":\"Estudiante\",\"apellidos\":\"De Prueba\",\"seccionId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.codigo").value("COD009"))
                .andExpect(jsonPath("$.seccion.id").value(2))
                .andExpect(jsonPath("$.qrToken").doesNotExist());
    }

    @Test
    void putEstudianteComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(put("/api/v1/estudiantes/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"COD001\",\"nombres\":\"Estudiante\",\"apellidos\":\"De Prueba\",\"seccionId\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void putEstudianteInexistenteDevuelve404() throws Exception {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/estudiantes/99")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"COD001\",\"nombres\":\"Estudiante\",\"apellidos\":\"De Prueba\",\"seccionId\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void putEstudianteConSeccionInexistenteDevuelve404() throws Exception {
        when(estudianteRepository.findById(5L))
                .thenReturn(Optional.of(estudiante(5L, "COD001", seccion(1L, "A"))));
        when(seccionRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/estudiantes/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"COD001\",\"nombres\":\"Estudiante\",\"apellidos\":\"De Prueba\",\"seccionId\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SECTION_NOT_FOUND"));
    }

    @Test
    void putEstudianteConCodigoDuplicadoDevuelve409() throws Exception {
        when(estudianteRepository.findById(5L))
                .thenReturn(Optional.of(estudiante(5L, "COD001", seccion(1L, "A"))));
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo("COD002")).thenReturn(true);

        mockMvc.perform(put("/api/v1/estudiantes/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"COD002\",\"nombres\":\"Estudiante\",\"apellidos\":\"De Prueba\",\"seccionId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDENT_CODE_ALREADY_EXISTS"));
    }

    @Test
    void patchDesactivarComoAdminDevuelve200() throws Exception {
        Estudiante existente = estudiante(5L, "COD001", seccion(1L, "A"));
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/v1/estudiantes/5/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.codigo").value("COD001"));
    }

    @Test
    void patchDesactivarComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(patch("/api/v1/estudiantes/5/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void patchDesactivarInexistenteDevuelve404() throws Exception {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/estudiantes/99/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void dobleDesactivacionEsIdempotente() throws Exception {
        Estudiante existente = estudiante(5L, "COD001", seccion(1L, "A"));
        existente.setActivo(false);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/v1/estudiantes/5/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void getEstudiantesFiltroCodigo() throws Exception {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(estudiante(1L, "COD001", seccion(1L, "A"))));

        mockMvc.perform(get("/api/v1/estudiantes").param("codigo", "COD001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("COD001"));

        verify(estudianteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void getEstudiantesFiltroNombre() throws Exception {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(estudiante(1L, "COD001", seccion(1L, "A"))));

        mockMvc.perform(get("/api/v1/estudiantes").param("nombre", "Estudiante")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        verify(estudianteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void getEstudiantesFiltroSeccion() throws Exception {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(estudiante(1L, "COD001", seccion(1L, "A"))));

        mockMvc.perform(get("/api/v1/estudiantes").param("seccion", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        verify(estudianteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void getEstudiantesFiltroActivo() throws Exception {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(estudiante(1L, "COD001", seccion(1L, "A"))));

        mockMvc.perform(get("/api/v1/estudiantes").param("activo", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        verify(estudianteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void getEstudiantesCombinacionDeFiltros() throws Exception {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(estudiante(1L, "COD001", seccion(1L, "A"))));

        mockMvc.perform(get("/api/v1/estudiantes")
                        .param("codigo", "COD001")
                        .param("seccion", "1")
                        .param("activo", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("COD001"));

        verify(estudianteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void postRegenerarQrComoAdminDevuelve200() throws Exception {
        when(estudianteRepository.findById(5L))
                .thenReturn(Optional.of(estudiante(5L, "COD001", seccion(1L, "A"))));
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/estudiantes/5/regenerar-qr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.qrToken").doesNotExist());
    }

    @Test
    void postRegenerarQrComoDocenteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/estudiantes/5/regenerar-qr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void postRegenerarQrSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/estudiantes/5/regenerar-qr"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void postRegenerarQrEstudianteInexistenteDevuelve404() throws Exception {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/estudiantes/99/regenerar-qr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void getQrComoAdminDevuelvePngDecodificable() throws Exception {
        when(estudianteRepository.findById(5L))
                .thenReturn(Optional.of(estudiante(5L, "COD001", seccion(1L, "A"))));

        MvcResult resultado = mockMvc.perform(get("/api/v1/estudiantes/5/qr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn();

        byte[] png = resultado.getResponse().getContentAsByteArray();
        assertThat(png).isNotEmpty();
        assertThat(png).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
        assertThat(decodificar(png)).isEqualTo("AULAIA:STUDENT:TOKEN_OPACO");
    }

    @Test
    void getQrComoDocenteDevuelvePng() throws Exception {
        when(estudianteRepository.findById(5L))
                .thenReturn(Optional.of(estudiante(5L, "COD001", seccion(1L, "A"))));

        mockMvc.perform(get("/api/v1/estudiantes/5/qr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + docenteToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void getQrSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes/5/qr"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getQrEstudianteInexistenteDevuelve404Json() throws Exception {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/estudiantes/99/qr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    private Seccion seccion(Long id, String nombre) {
        Seccion seccion = new Seccion();
        seccion.setId(id);
        seccion.setNombre(nombre);
        return seccion;
    }

    private Estudiante estudiante(Long id, String codigo, Seccion seccion) {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(id);
        estudiante.setCodigo(codigo);
        estudiante.setQrToken("TOKEN_OPACO");
        estudiante.setNombres("Estudiante");
        estudiante.setApellidos("De Prueba");
        estudiante.setSeccion(seccion);
        estudiante.setActivo(true);
        return estudiante;
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

    private String decodificar(byte[] png) throws Exception {
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen)));
        return new MultiFormatReader().decode(bitmap).getText();
    }
}
