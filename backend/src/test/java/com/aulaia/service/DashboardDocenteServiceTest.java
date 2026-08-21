package com.aulaia.service;

import com.aulaia.dto.dashboard.DashboardDocenteResponse;
import com.aulaia.dto.sesion.SesionClaseResponse;
import com.aulaia.entity.*;
import com.aulaia.mapper.SesionClaseMapper;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.HorarioRepository;
import com.aulaia.repository.SesionClaseRepository;
import com.aulaia.repository.UsuarioRepository;
import com.aulaia.repository.DocenteRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardDocenteServiceTest {

    @Mock
    private HorarioRepository horarioRepository;
    @Mock
    private SesionClaseRepository sesionClaseRepository;
    @Mock
    private AsistenciaRepository asistenciaRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.AuditoriaRepository auditoriaRepository;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private SesionClaseMapper sesionClaseMapper;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private DashboardDocenteService dashboardDocenteService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obtenerResumen_ConSesionAbierta_CalculaMetricas() {
        // Arrange
        Horario horario = new Horario();
        horario.setId(10L);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(10, 0));
        
        Docente docente = new Docente();
        docente.setId(1L);
        docente.setNombres("Juan");
        docente.setApellidos("Perez");
        horario.setDocente(docente);
        
        Seccion seccion = new Seccion();
        seccion.setId(5L);
        seccion.setNombre("6A");
        horario.setSeccion(seccion);
        
        Curso curso = new Curso();
        curso.setId(2L);
        curso.setNombre("Mate");
        horario.setCurso(curso);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("docente@aulaia.com");
        
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        when(usuarioRepository.findByUsername("docente@aulaia.com")).thenReturn(Optional.of(usuario));
        
        Docente docenteResult = new Docente();
        docenteResult.setId(1L);
        when(docenteRepository.findByUsuarioId(99L)).thenReturn(Optional.of(docenteResult));

        when(horarioRepository.buscarConFiltros(eq(1L), isNull(), isNull(), anyShort()))
                .thenReturn(List.of(horario));

        SesionClase sesion = new SesionClase();
        sesion.setId(100L);
        sesion.setEstado(SesionClaseEstado.ABIERTA);

        when(sesionClaseRepository.findByHorarioIdAndFecha(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(sesion));

        SesionClaseResponse mockedResponse = new SesionClaseResponse(
                100L, 10L, LocalDate.now(), SesionClaseEstado.ABIERTA, null, null, null, null, new SesionClaseResponse.CursoResumen(2L, "Mate"),
                new SesionClaseResponse.SeccionResumen(5L, "6A"),
                new SesionClaseResponse.DocenteResumen(1L, "Juan", "Perez")
        );
        when(sesionClaseMapper.toResponse(sesion)).thenReturn(mockedResponse);

        when(estudianteRepository.countBySeccionIdAndActivoTrue(5L)).thenReturn(10L);

        Asistencia a1 = new Asistencia();
        a1.setEstado(EstadoAsistencia.PRESENTE);
        Asistencia a2 = new Asistencia();
        a2.setEstado(EstadoAsistencia.TARDANZA);
        Asistencia a3 = new Asistencia();
        a3.setEstado(EstadoAsistencia.AUSENTE);

        when(asistenciaRepository.findBySesionClaseId(100L)).thenReturn(List.of(a1, a2, a3));

        // Act
        DashboardDocenteResponse response = dashboardDocenteService.obtenerResumen();

        // Assert
        assertNotNull(response);
        assertEquals(1, response.estadisticas().presentes());
        assertEquals(1, response.estadisticas().tardanzas());
        assertEquals(1, response.estadisticas().ausentes());
        assertEquals(10, response.estadisticas().totalEstudiantes());
        assertEquals(20.0, response.estadisticas().porcentajeAsistencia()); // 2/10 = 20%
        
        assertNotNull(response.claseActual());
        assertEquals(100L, response.claseActual().id());
        assertEquals(1, response.clasesDelDia().size());
    }
}
