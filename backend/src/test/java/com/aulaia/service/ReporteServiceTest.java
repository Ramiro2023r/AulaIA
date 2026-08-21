package com.aulaia.service;

import com.aulaia.dto.ReporteAsistenciaDto;
import com.aulaia.dto.ReporteFiltrosDto;
import com.aulaia.entity.Asistencia;
import com.aulaia.entity.Curso;
import com.aulaia.entity.EstadoAsistencia;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.Horario;
import com.aulaia.entity.Seccion;
import com.aulaia.entity.SesionClase;
import com.aulaia.repository.AsistenciaRepository;
import com.aulaia.repository.JustificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private AsistenciaRepository asistenciaRepository;

    @Mock
    private JustificacionRepository justificacionRepository;

    @InjectMocks
    private ReporteService reporteService;

    private Asistencia asistenciaMock;

    @BeforeEach
    void setUp() {
        Curso curso = new Curso();
        curso.setNombre("Matemática");

        Seccion seccion = new Seccion();
        seccion.setNombre("A");

        Horario horario = new Horario();
        horario.setCurso(curso);
        horario.setSeccion(seccion);

        SesionClase sesionClase = new SesionClase();
        sesionClase.setHorario(horario);
        sesionClase.setFecha(LocalDate.of(2026, 8, 20));

        Estudiante estudiante = new Estudiante();
        estudiante.setNombres("Juan");
        estudiante.setApellidos("Pérez");

        asistenciaMock = new Asistencia();
        asistenciaMock.setId(1L);
        asistenciaMock.setSesionClase(sesionClase);
        asistenciaMock.setEstudiante(estudiante);
        asistenciaMock.setEstado(EstadoAsistencia.PRESENTE);
    }

    @Test
    void generarReporteAsistencias_conFiltros_retornaLista() {
        // Arrange
        ReporteFiltrosDto filtros = new ReporteFiltrosDto();
        filtros.setFechaInicio(LocalDate.of(2026, 8, 1));
        filtros.setFechaFin(LocalDate.of(2026, 8, 30));
        
        when(asistenciaRepository.findAll(any(Specification.class))).thenReturn(List.of(asistenciaMock));
        when(justificacionRepository.findByAsistenciaIdIn(any())).thenReturn(new ArrayList<>());

        // Act
        List<ReporteAsistenciaDto> resultado = reporteService.generarReporteAsistencias(filtros);

        // Assert
        assertEquals(1, resultado.size());
        assertEquals("Juan Pérez", resultado.get(0).getEstudianteNombreCompleto());
        assertEquals("Matemática", resultado.get(0).getCursoNombre());
        assertEquals("A", resultado.get(0).getSeccionNombre());
        assertEquals("PRESENTE", resultado.get(0).getEstadoAsistencia());
    }

    @Test
    void generarReporteAsistencias_sinResultados_retornaVacio() {
        // Arrange
        ReporteFiltrosDto filtros = new ReporteFiltrosDto();
        when(asistenciaRepository.findAll(any(Specification.class))).thenReturn(new ArrayList<>());

        // Act
        List<ReporteAsistenciaDto> resultado = reporteService.generarReporteAsistencias(filtros);

        // Assert
        assertEquals(0, resultado.size());
    }
}
