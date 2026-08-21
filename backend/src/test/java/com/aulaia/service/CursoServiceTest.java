package com.aulaia.service;

import com.aulaia.dto.curso.CursoRequest;
import com.aulaia.dto.curso.CursoResponse;
import com.aulaia.entity.Curso;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.CursoMapperImpl;
import com.aulaia.repository.CursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link CursoService} (Prompt 3.3), con repositorio
 * mockeado y {@link CursoMapperImpl} real. No dependen de PostgreSQL.
 *
 * <p>Sin regla de unicidad (04-BD §6.6 no define UNIQUE sobre nombre): el
 * servicio permite crear dos cursos con el mismo nombre y no genera 409.
 */
class CursoServiceTest {

    private CursoRepository cursoRepository;
    private CursoService cursoService;

    @BeforeEach
    void setUp() {
        cursoRepository = mock(CursoRepository.class);
        cursoService = new CursoService(cursoRepository, new CursoMapperImpl());
    }

    private Curso curso(Long id, String nombre, String descripcion) {
        Curso curso = new Curso();
        curso.setId(id);
        curso.setNombre(nombre);
        curso.setDescripcion(descripcion);
        return curso;
    }

    private CursoRequest request(String nombre, String descripcion) {
        return new CursoRequest(nombre, descripcion);
    }

    @Test
    void crearCursoCorrectamente() {
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> {
            Curso c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CursoResponse response = cursoService.crear(request(" Computación ", " Curso de ofimática "));

        ArgumentCaptor<Curso> captor = ArgumentCaptor.forClass(Curso.class);
        verify(cursoRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Computación");
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Curso de ofimática");
        assertThat(captor.getValue().isActivo()).isTrue();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.nombre()).isEqualTo("Computación");
        assertThat(response.descripcion()).isEqualTo("Curso de ofimática");
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crearCursoSinDescripcionPermitido() {
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

        CursoResponse response = cursoService.crear(request("Matemáticas", null));

        assertThat(response.nombre()).isEqualTo("Matemáticas");
        assertThat(response.descripcion()).isNull();
    }

    @Test
    void crearDosCursosConElMismoNombrePermitido() {
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

        CursoResponse primero = cursoService.crear(request("Computación", null));
        CursoResponse segundo = cursoService.crear(request("Computación", null));

        assertThat(primero.nombre()).isEqualTo(segundo.nombre());
        verify(cursoRepository, times(2)).save(any(Curso.class));
    }

    @Test
    void buscarCursoExistente() {
        when(cursoRepository.findById(1L))
                .thenReturn(Optional.of(curso(1L, "Computación", "Curso de ofimática")));

        CursoResponse response = cursoService.buscarPorId(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Computación");
        assertThat(response.descripcion()).isEqualTo("Curso de ofimática");
    }

    @Test
    void buscarCursoInexistenteLanzaNotFound() {
        when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cursoService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("COURSE_NOT_FOUND");
    }

    @Test
    void listarCursosDevuelveEnOrdenEstable() {
        when(cursoRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(
                        curso(1L, "Computación", null),
                        curso(2L, "Matemáticas", "Curso de álgebra")));

        List<CursoResponse> response = cursoService.listar();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).id()).isEqualTo(1L);
        assertThat(response.get(1).id()).isEqualTo(2L);
        assertThat(response.get(1).descripcion()).isEqualTo("Curso de álgebra");
    }

    @Test
    void actualizarCursoCorrectamente() {
        Curso existente = curso(5L, "Computación", "Descripción anterior");
        when(cursoRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

        CursoResponse response = cursoService.actualizar(5L, request(" Matemáticas ", " Álgebra lineal "));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.nombre()).isEqualTo("Matemáticas");
        assertThat(response.descripcion()).isEqualTo("Álgebra lineal");
        assertThat(existente.getNombre()).isEqualTo("Matemáticas");
        assertThat(existente.getDescripcion()).isEqualTo("Álgebra lineal");
    }

    @Test
    void actualizarCursoInexistenteLanzaNotFound() {
        when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cursoService.actualizar(99L, request("Matemáticas", null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("COURSE_NOT_FOUND");
        verify(cursoRepository, never()).save(any());
    }
}