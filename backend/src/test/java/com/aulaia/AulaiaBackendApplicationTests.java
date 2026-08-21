package com.aulaia;

import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.CursoRepository;
import com.aulaia.repository.DocenteRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.GradoRepository;
import com.aulaia.repository.HorarioRepository;
import com.aulaia.repository.SesionClaseRepository;
import com.aulaia.repository.SeccionRepository;
import com.aulaia.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Prueba inicial: valida que el contexto Spring cargue correctamente
 * sin depender de una base de datos externa (perfil de prueba aislado).
 */
@SpringBootTest
@ActiveProfiles("test")
class AulaiaBackendApplicationTests {

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

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.AuditoriaRepository auditoriaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    @Test
    void contextLoads() {
    }
}