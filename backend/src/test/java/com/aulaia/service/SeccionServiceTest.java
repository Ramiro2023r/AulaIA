package com.aulaia.service;

import com.aulaia.dto.seccion.SeccionRequest;
import com.aulaia.dto.seccion.SeccionResponse;
import com.aulaia.entity.Grado;
import com.aulaia.entity.Seccion;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.SeccionMapperImpl;
import com.aulaia.repository.GradoRepository;
import com.aulaia.repository.SeccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link SeccionService} (Prompt 3.2), con repositorios
 * mockeados y {@link SeccionMapperImpl} real. No dependen de PostgreSQL.
 */
class SeccionServiceTest {

    private SeccionRepository seccionRepository;
    private GradoRepository gradoRepository;
    private SeccionService seccionService;

    @BeforeEach
    void setUp() {
        seccionRepository = mock(SeccionRepository.class);
        gradoRepository = mock(GradoRepository.class);
        seccionService = new SeccionService(seccionRepository, gradoRepository, org.mapstruct.factory.Mappers.getMapper(com.aulaia.mapper.SeccionMapper.class));
    }

    private Grado grado(Long id, String nombre) {
        Grado grado = new Grado();
        grado.setId(id);
        grado.setNombre(nombre);
        return grado;
    }

    private Seccion seccion(Long id, Grado grado, String nombre, String periodo) {
        Seccion seccion = new Seccion();
        seccion.setId(id);
        seccion.setGrado(grado);
        seccion.setNombre(nombre);
        seccion.setPeriodoAcademico(periodo);
        return seccion;
    }

    private SeccionRequest request(Long gradoId, String nombre, String periodo) {
        return new SeccionRequest(gradoId, nombre, periodo);
    }

    private DataIntegrityViolationException violacionUnique() {
        return new DataIntegrityViolationException("could not execute statement",
                new RuntimeException(new SQLException(
                        "ERROR: duplicate key value violates unique constraint \"uq_seccion_grado_periodo\""
                                + " Detail: Key (grado_id, nombre, periodo_academico)=(1, A, 2026) already exists.")));
    }

    @Test
    void crearSeccionCorrectamente() {
        when(gradoRepository.findById(1L)).thenReturn(Optional.of(grado(1L, "6.º Primaria")));
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademico(1L, "A", "2026"))
                .thenReturn(false);
        when(seccionRepository.saveAndFlush(any(Seccion.class))).thenAnswer(inv -> {
            Seccion s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        SeccionResponse response = seccionService.crear(request(1L, " A ", " 2026 "));

        ArgumentCaptor<Seccion> captor = ArgumentCaptor.forClass(Seccion.class);
        verify(seccionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("A");
        assertThat(captor.getValue().getPeriodoAcademico()).isEqualTo("2026");
        assertThat(captor.getValue().getGrado().getId()).isEqualTo(1L);
        assertThat(captor.getValue().isActivo()).isTrue();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.grado().id()).isEqualTo(1L);
        assertThat(response.grado().nombre()).isEqualTo("6.º Primaria");
        assertThat(response.nombre()).isEqualTo("A");
        assertThat(response.periodoAcademico()).isEqualTo("2026");
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crearConGradoInexistenteLanzaGradeNotFound() {
        when(gradoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seccionService.crear(request(99L, "A", "2026")))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("GRADE_NOT_FOUND");
        verify(seccionRepository, never()).saveAndFlush(any());
    }

    @Test
    void crearDuplicadaLanzaConflict() {
        when(gradoRepository.findById(1L)).thenReturn(Optional.of(grado(1L, "6.º Primaria")));
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademico(1L, "A", "2026"))
                .thenReturn(true);

        assertThatThrownBy(() -> seccionService.crear(request(1L, "A", "2026")))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("SECTION_ALREADY_EXISTS");
        verify(seccionRepository, never()).saveAndFlush(any());
    }

    @Test
    void crearMismoNombreDistintoGradoPermitido() {
        when(gradoRepository.findById(1L)).thenReturn(Optional.of(grado(1L, "5.º Primaria")));
        when(gradoRepository.findById(2L)).thenReturn(Optional.of(grado(2L, "6.º Primaria")));
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademico(anyLong(), any(), any()))
                .thenReturn(false);
        when(seccionRepository.saveAndFlush(any(Seccion.class))).thenAnswer(inv -> inv.getArgument(0));

        SeccionResponse grado5 = seccionService.crear(request(1L, "A", "2026"));
        SeccionResponse grado6 = seccionService.crear(request(2L, "A", "2026"));

        assertThat(grado5.grado().id()).isEqualTo(1L);
        assertThat(grado6.grado().id()).isEqualTo(2L);
        assertThat(grado5.nombre()).isEqualTo(grado6.nombre());
        verify(seccionRepository, times(2)).saveAndFlush(any(Seccion.class));
    }

    @Test
    void crearMismoNombreMismoGradoOtroPeriodoPermitido() {
        when(gradoRepository.findById(1L)).thenReturn(Optional.of(grado(1L, "6.º Primaria")));
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademico(anyLong(), any(), any()))
                .thenReturn(false);
        when(seccionRepository.saveAndFlush(any(Seccion.class))).thenAnswer(inv -> inv.getArgument(0));

        SeccionResponse periodo2026 = seccionService.crear(request(1L, "A", "2026"));
        SeccionResponse periodo2027 = seccionService.crear(request(1L, "A", "2027"));

        assertThat(periodo2026.periodoAcademico()).isEqualTo("2026");
        assertThat(periodo2027.periodoAcademico()).isEqualTo("2027");
        verify(seccionRepository, times(2)).saveAndFlush(any(Seccion.class));
    }

    @Test
    void crearPermiteNombreAMayusculaYMinusculaEnElMismoGradoYPeriodo() {
        when(gradoRepository.findById(1L)).thenReturn(Optional.of(grado(1L, "6.º Primaria")));
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademico(1L, "A", "2026"))
                .thenReturn(false);
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademico(1L, "a", "2026"))
                .thenReturn(false);
        when(seccionRepository.saveAndFlush(any(Seccion.class))).thenAnswer(inv -> inv.getArgument(0));

        SeccionResponse mayuscula = seccionService.crear(request(1L, "A", "2026"));
        SeccionResponse minuscula = seccionService.crear(request(1L, "a", "2026"));

        assertThat(mayuscula.nombre()).isEqualTo("A");
        assertThat(minuscula.nombre()).isEqualTo("a");
        verify(seccionRepository, times(2)).saveAndFlush(any(Seccion.class));
    }

    @Test
    void crearConViolacionUniqueDeBdLanzaConflict() {
        when(gradoRepository.findById(1L)).thenReturn(Optional.of(grado(1L, "6.º Primaria")));
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademico(1L, "A", "2026"))
                .thenReturn(false);
        when(seccionRepository.saveAndFlush(any(Seccion.class))).thenThrow(violacionUnique());

        assertThatThrownBy(() -> seccionService.crear(request(1L, "A", "2026")))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("SECTION_ALREADY_EXISTS");
    }

    @Test
    void buscarSeccionExistente() {
        when(seccionRepository.findById(1L))
                .thenReturn(Optional.of(seccion(1L, grado(1L, "6.º Primaria"), "A", "2026")));

        SeccionResponse response = seccionService.buscarPorId(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("A");
        assertThat(response.periodoAcademico()).isEqualTo("2026");
        assertThat(response.grado().nombre()).isEqualTo("6.º Primaria");
    }

    @Test
    void buscarSeccionInexistenteLanzaNotFound() {
        when(seccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seccionService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("SECTION_NOT_FOUND");
    }

    @Test
    void listarSeccionesDevuelveEnOrdenEstable() {
        when(seccionRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(
                        seccion(1L, grado(1L, "5.º Primaria"), "A", "2026"),
                        seccion(2L, grado(2L, "6.º Primaria"), "B", "2026")));

        List<SeccionResponse> response = seccionService.listar();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).id()).isEqualTo(1L);
        assertThat(response.get(1).id()).isEqualTo(2L);
        assertThat(response.get(1).grado().nombre()).isEqualTo("6.º Primaria");
    }

    @Test
    void actualizarSeccionCorrectamente() {
        Seccion existente = seccion(5L, grado(1L, "5.º Primaria"), "A", "2026");
        when(seccionRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(gradoRepository.findById(2L)).thenReturn(Optional.of(grado(2L, "6.º Primaria")));
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademicoAndIdNot(2L, "B", "2027", 5L))
                .thenReturn(false);
        when(seccionRepository.saveAndFlush(any(Seccion.class))).thenAnswer(inv -> inv.getArgument(0));

        SeccionResponse response = seccionService.actualizar(5L, request(2L, " B ", " 2027 "));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.nombre()).isEqualTo("B");
        assertThat(response.periodoAcademico()).isEqualTo("2027");
        assertThat(response.grado().id()).isEqualTo(2L);
        assertThat(existente.getGrado().getId()).isEqualTo(2L);
    }

    @Test
    void actualizarSeccionInexistenteLanzaNotFound() {
        when(seccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seccionService.actualizar(99L, request(1L, "A", "2026")))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("SECTION_NOT_FOUND");
        verify(seccionRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizarConGradoInexistenteLanzaGradeNotFound() {
        when(seccionRepository.findById(5L))
                .thenReturn(Optional.of(seccion(5L, grado(1L, "5.º Primaria"), "A", "2026")));
        when(gradoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seccionService.actualizar(5L, request(99L, "A", "2026")))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("GRADE_NOT_FOUND");
        verify(seccionRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizarGenerandoDuplicadoLanzaConflict() {
        when(seccionRepository.findById(5L))
                .thenReturn(Optional.of(seccion(5L, grado(1L, "5.º Primaria"), "A", "2026")));
        when(gradoRepository.findById(2L)).thenReturn(Optional.of(grado(2L, "6.º Primaria")));
        when(seccionRepository.existsByGradoIdAndNombreAndPeriodoAcademicoAndIdNot(2L, "B", "2026", 5L))
                .thenReturn(true);

        assertThatThrownBy(() -> seccionService.actualizar(5L, request(2L, "B", "2026")))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("SECTION_ALREADY_EXISTS");
        verify(seccionRepository, never()).saveAndFlush(any());
    }
}