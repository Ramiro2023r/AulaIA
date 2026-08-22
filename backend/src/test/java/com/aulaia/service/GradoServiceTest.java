package com.aulaia.service;

import com.aulaia.dto.grado.GradoRequest;
import com.aulaia.dto.grado.GradoResponse;
import com.aulaia.entity.Grado;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.GradoMapperImpl;
import com.aulaia.repository.GradoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
 * Pruebas unitarias de {@link GradoService} (Prompt 3.1), con repositorio
 * mockeado y {@link GradoMapperImpl} real. No dependen de PostgreSQL.
 */
class GradoServiceTest {

    private GradoRepository gradoRepository;
    private GradoService gradoService;

    @BeforeEach
    void setUp() {
        gradoRepository = mock(GradoRepository.class);
        gradoService = new GradoService(gradoRepository, org.mapstruct.factory.Mappers.getMapper(com.aulaia.mapper.GradoMapper.class));
    }

    private Grado grado(Long id, String nombre, String nivel) {
        Grado grado = new Grado();
        grado.setId(id);
        grado.setNombre(nombre);
        grado.setNivel(nivel);
        grado.setOrden(1);
        return grado;
    }

    @Test
    void crearGradoCorrectamente() {
        when(gradoRepository.save(any(Grado.class))).thenAnswer(inv -> {
            Grado g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });

        GradoResponse response = gradoService.crear(new GradoRequest(" 5.º Primaria ", null, 1));

        ArgumentCaptor<Grado> captor = ArgumentCaptor.forClass(Grado.class);
        verify(gradoRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("5.º Primaria");
        assertThat(captor.getValue().getNivel()).isEqualTo("PRIMARIA");
        assertThat(captor.getValue().getOrden()).isEqualTo(1);
        assertThat(captor.getValue().isActivo()).isTrue();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.nombre()).isEqualTo("5.º Primaria");
        assertThat(response.nivel()).isEqualTo("PRIMARIA");
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crearGradoConNivelExplicito() {
        when(gradoRepository.save(any(Grado.class))).thenAnswer(inv -> inv.getArgument(0));

        GradoResponse response = gradoService.crear(new GradoRequest("6.º Primaria", "PRIMARIA", null));

        assertThat(response.nivel()).isEqualTo("PRIMARIA");
        assertThat(response.orden()).isNull();
    }

    @Test
    void crearPermiteDosGradosConElMismoNombre() {
        when(gradoRepository.save(any(Grado.class))).thenAnswer(inv -> inv.getArgument(0));

        GradoResponse primero = gradoService.crear(new GradoRequest("5.º Primaria", null, null));
        GradoResponse segundo = gradoService.crear(new GradoRequest("5.º Primaria", null, null));

        assertThat(primero.nombre()).isEqualTo("5.º Primaria");
        assertThat(segundo.nombre()).isEqualTo("5.º Primaria");
        verify(gradoRepository, times(2)).save(any(Grado.class));
    }

    @Test
    void buscarGradoExistente() {
        when(gradoRepository.findById(1L)).thenReturn(Optional.of(grado(1L, "5.º Primaria", "PRIMARIA")));

        GradoResponse response = gradoService.buscarPorId(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("5.º Primaria");
        assertThat(response.nivel()).isEqualTo("PRIMARIA");
    }

    @Test
    void buscarGradoInexistenteLanzaNotFound() {
        when(gradoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradoService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("GRADE_NOT_FOUND");
    }

    @Test
    void listarGradosDevuelveEnOrdenEstable() {
        when(gradoRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(grado(1L, "5.º Primaria", "PRIMARIA"), grado(2L, "6.º Primaria", "PRIMARIA")));

        List<GradoResponse> response = gradoService.listar();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).id()).isEqualTo(1L);
        assertThat(response.get(1).id()).isEqualTo(2L);
    }

    @Test
    void actualizarGradoCorrectamente() {
        Grado existente = grado(5L, "Viejo nombre", "PRIMARIA");
        when(gradoRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(gradoRepository.save(any(Grado.class))).thenAnswer(inv -> inv.getArgument(0));

        GradoResponse response = gradoService.actualizar(5L, new GradoRequest("Nuevo nombre", "SECUNDARIA", 2));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.nombre()).isEqualTo("Nuevo nombre");
        assertThat(response.nivel()).isEqualTo("SECUNDARIA");
        assertThat(response.orden()).isEqualTo(2);
        assertThat(existente.getId()).isEqualTo(5L);
    }

    @Test
    void actualizarGradoInexistenteLanzaNotFound() {
        when(gradoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradoService.actualizar(99L, new GradoRequest("X", null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("GRADE_NOT_FOUND");
        verify(gradoRepository, never()).save(any());
    }

    @Test
    void actualizarPermiteNombreIgualAOtroGrado() {
        Grado existente = grado(5L, "5.º Primaria", "PRIMARIA");
        when(gradoRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(gradoRepository.save(any(Grado.class))).thenAnswer(inv -> inv.getArgument(0));

        GradoResponse response = gradoService.actualizar(5L, new GradoRequest("6.º Primaria", null, null));

        assertThat(response.nombre()).isEqualTo("6.º Primaria");
    }

    @Test
    void crearConNivelEnBlancoUsaDefaultPrimaria() {
        when(gradoRepository.save(any(Grado.class))).thenAnswer(inv -> inv.getArgument(0));

        GradoResponse response = gradoService.crear(new GradoRequest("3.º Primaria", "   ", null));

        assertThat(response.nivel()).isEqualTo("PRIMARIA");
    }
}