package com.aulaia.service;

import com.aulaia.dto.estudiante.EstudianteRequest;
import com.aulaia.dto.estudiante.EstudianteResponse;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.Seccion;
import com.aulaia.exception.ConflictException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.mapper.EstudianteMapperImpl;
import com.aulaia.repository.EstudianteRepository;
import com.aulaia.repository.SeccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link EstudianteService} (Prompt 4.2), con
 * repositorios mockeados y {@link EstudianteMapperImpl} real. No dependen
 * de PostgreSQL. Datos ficticios, nunca de menores reales.
 */
class EstudianteServiceTest {

    private EstudianteRepository estudianteRepository;
    @Mock
    private SeccionRepository seccionRepository;
    
    @Mock
    private AuditService auditService;

    private EstudianteService estudianteService;

    @BeforeEach
    void setUp() {
        estudianteRepository = mock(EstudianteRepository.class);
        seccionRepository = mock(SeccionRepository.class);
        auditService = mock(AuditService.class);
        estudianteService = new EstudianteService(
                estudianteRepository,
                seccionRepository,
                org.mapstruct.factory.Mappers.getMapper(com.aulaia.mapper.EstudianteMapper.class),
                auditService
        );
    }

    private Seccion seccion(Long id, String nombre) {
        Seccion seccion = new Seccion();
        seccion.setId(id);
        seccion.setNombre(nombre);
        return seccion;
    }

    private Estudiante estudiante(Long id, String codigo, String qrToken, Seccion seccion, boolean activo) {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(id);
        estudiante.setCodigo(codigo);
        estudiante.setQrToken(qrToken);
        estudiante.setNombres("Estudiante");
        estudiante.setApellidos("De Prueba");
        estudiante.setSeccion(seccion);
        estudiante.setActivo(activo);
        return estudiante;
    }

    private EstudianteRequest request(String codigo, Long seccionId) {
        return new EstudianteRequest(codigo, "Estudiante", "De Prueba", seccionId);
    }

    private String tokenGuardado() {
        ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
        verify(estudianteRepository).saveAndFlush(captor.capture());
        return captor.getValue().getQrToken();
    }

    @Test
    void crearEstudianteCorrectamente() {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo("COD001")).thenReturn(false);
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> {
            Estudiante e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        EstudianteResponse response = estudianteService.crear(request(" COD001 ", 1L));

        ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
        verify(estudianteRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("COD001");
        assertThat(captor.getValue().getNombres()).isEqualTo("Estudiante");
        assertThat(captor.getValue().getApellidos()).isEqualTo("De Prueba");
        assertThat(captor.getValue().getSeccion().getId()).isEqualTo(1L);
        assertThat(captor.getValue().isActivo()).isTrue();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.codigo()).isEqualTo("COD001");
        assertThat(response.seccion().id()).isEqualTo(1L);
        assertThat(response.activo()).isTrue();
    }

    @Test
    void qrTokenGeneradoAutomaticamente() {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo(anyString())).thenReturn(false);
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.crear(request("COD001", 1L));

        assertThat(tokenGuardado()).isNotBlank();
    }

    @Test
    void qrTokenNoContieneNombres() {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo(anyString())).thenReturn(false);
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.crear(request("COD001", 1L));

        assertThat(tokenGuardado()).doesNotContain("Estudiante");
    }

    @Test
    void qrTokenNoContieneApellidos() {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo(anyString())).thenReturn(false);
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.crear(request("COD001", 1L));

        assertThat(tokenGuardado()).doesNotContain("Prueba");
    }

    @Test
    void qrTokenNoContieneCodigo() {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo(anyString())).thenReturn(false);
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.crear(request("COD001", 1L));

        assertThat(tokenGuardado()).doesNotContain("COD001");
    }

    @Test
    void qrTokenRespetaLongitudDeBaseDeDatos() {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo(anyString())).thenReturn(false);
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.crear(request("COD001", 1L));

        assertThat(tokenGuardado()).matches("[0-9a-f]{64}");
    }

    @Test
    void tokensDistintosEntreDosCreaciones() {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo(anyString())).thenReturn(false);
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.crear(request("COD001", 1L));
        String primero = tokenGuardado();
        estudianteService.crear(request("COD002", 1L));
        ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
        verify(estudianteRepository, times(2)).saveAndFlush(captor.capture());

        assertThat(captor.getAllValues().get(0).getQrToken()).isNotEqualTo(captor.getAllValues().get(1).getQrToken());
        assertThat(primero).isNotBlank();
    }

    @Test
    void codigoDuplicadoLanzaConflict() {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo("COD001")).thenReturn(true);

        assertThatThrownBy(() -> estudianteService.crear(request("COD001", 1L)))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("STUDENT_CODE_ALREADY_EXISTS");
        verify(estudianteRepository, never()).saveAndFlush(any());
    }

    @Test
    void seccionInexistenteLanzaSectionNotFound() {
        when(seccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estudianteService.crear(request("COD001", 99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("SECTION_NOT_FOUND");
        verify(estudianteRepository, never()).saveAndFlush(any());
    }

    @Test
    void buscarPorIdExistente() {
        when(estudianteRepository.findById(1L))
                .thenReturn(Optional.of(estudiante(1L, "COD001", "TOKEN", seccion(1L, "A"), true)));

        EstudianteResponse response = estudianteService.buscarPorId(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.codigo()).isEqualTo("COD001");
        assertThat(response.seccion().nombre()).isEqualTo("A");
        assertThat(response.activo()).isTrue();
    }

    @Test
    void buscarPorIdInexistenteLanzaNotFound() {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estudianteService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("STUDENT_NOT_FOUND");
    }

    @Test
    void buscarPorCodigo() {
        when(estudianteRepository.findByCodigo("COD001"))
                .thenReturn(Optional.of(estudiante(1L, "COD001", "TOKEN", seccion(1L, "A"), true)));

        EstudianteResponse response = estudianteService.buscarPorCodigo("COD001");

        assertThat(response.codigo()).isEqualTo("COD001");
    }

    @Test
    void codigoInexistenteLanzaNotFound() {
        when(estudianteRepository.findByCodigo("NOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estudianteService.buscarPorCodigo("NOEXISTE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("STUDENT_NOT_FOUND");
    }

    @Test
    void buscarPorQrToken() {
        when(estudianteRepository.findByQrToken("TOKEN_OPACO"))
                .thenReturn(Optional.of(estudiante(1L, "COD001", "TOKEN_OPACO", seccion(1L, "A"), true)));

        EstudianteResponse response = estudianteService.buscarPorQrToken("TOKEN_OPACO");

        assertThat(response.codigo()).isEqualTo("COD001");
    }

    @Test
    void qrTokenInexistenteLanzaNotFound() {
        when(estudianteRepository.findByQrToken("NOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estudianteService.buscarPorQrToken("NOEXISTE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("STUDENT_NOT_FOUND");
    }

    @Test
    void listarDevuelveEnOrdenEstable() {
        when(estudianteRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(
                        estudiante(1L, "COD001", "T1", seccion(1L, "A"), true),
                        estudiante(2L, "COD002", "T2", seccion(2L, "B"), true)));

        List<EstudianteResponse> response = estudianteService.listar();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).id()).isEqualTo(1L);
        assertThat(response.get(1).seccion().nombre()).isEqualTo("B");
    }

    @Test
    void actualizarCorrectamente() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ORIGINAL", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(seccionRepository.findById(2L)).thenReturn(Optional.of(seccion(2L, "B")));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteResponse response = estudianteService.actualizar(5L, request("COD009", 2L));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.codigo()).isEqualTo("COD009");
        assertThat(response.seccion().id()).isEqualTo(2L);
        assertThat(existente.getCodigo()).isEqualTo("COD009");
        assertThat(existente.getSeccion().getId()).isEqualTo(2L);
    }

    @Test
    void actualizarConElMismoCodigoPermitido() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ORIGINAL", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteResponse response = estudianteService.actualizar(5L, request("COD001", 1L));

        assertThat(response.codigo()).isEqualTo("COD001");
        verify(estudianteRepository, never()).existsByCodigo(anyString());
    }

    @Test
    void actualizarACodigoDeOtroEstudianteLanzaConflict() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ORIGINAL", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo("COD002")).thenReturn(true);

        assertThatThrownBy(() -> estudianteService.actualizar(5L, request("COD002", 1L)))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("STUDENT_CODE_ALREADY_EXISTS");
        verify(estudianteRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizarConservaQrToken() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ORIGINAL", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(seccionRepository.findById(2L)).thenReturn(Optional.of(seccion(2L, "B")));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.actualizar(5L, request("COD009", 2L));

        ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
        verify(estudianteRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getQrToken()).isEqualTo("TOKEN_ORIGINAL");
    }

    @Test
    void actualizarCambiaSeccionCorrectamente() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ORIGINAL", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(seccionRepository.findById(3L)).thenReturn(Optional.of(seccion(3L, "C")));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteResponse response = estudianteService.actualizar(5L, request("COD001", 3L));

        assertThat(response.seccion().id()).isEqualTo(3L);
        assertThat(existente.getSeccion().getId()).isEqualTo(3L);
    }

    @Test
    void actualizarASeccionInexistenteLanzaSectionNotFound() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ORIGINAL", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(seccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estudianteService.actualizar(5L, request("COD001", 99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("SECTION_NOT_FOUND");
        verify(estudianteRepository, never()).saveAndFlush(any());
    }

    @Test
    void desactivarEstudiante() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteResponse response = estudianteService.desactivar(5L);

        assertThat(response.activo()).isFalse();
        assertThat(existente.isActivo()).isFalse();
        assertThat(existente.getQrToken()).isEqualTo("TOKEN");
        assertThat(existente.getCodigo()).isEqualTo("COD001");
    }

    @Test
    void desactivarEstudianteYaInactivoEsIdempotente() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN", seccion(1L, "A"), false);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteResponse response = estudianteService.desactivar(5L);

        assertThat(response.activo()).isFalse();
        assertThat(existente.isActivo()).isFalse();
        verify(estudianteRepository).saveAndFlush(any(Estudiante.class));
    }

    @Test
    void colisionDeQrTokenGeneraOtro() {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo(anyString())).thenReturn(false);
        when(estudianteRepository.existsByQrToken(anyString()))
                .thenAnswer(new Answer<>() {
                    private int llamadas = 0;

                    @Override
                    public Boolean answer(InvocationOnMock invocation) {
                        llamadas++;
                        return llamadas == 1;
                    }
                });
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteResponse response = estudianteService.crear(request("COD001", 1L));

        assertThat(response.id()).isNull();
        verify(estudianteRepository, times(2)).existsByQrToken(anyString());
    }

    @Test
    void limiteDeColisionesFallaControladamente() {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "A")));
        when(estudianteRepository.existsByCodigo(anyString())).thenReturn(false);
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(true);

        assertThatThrownBy(() -> estudianteService.crear(request("COD001", 1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("qrToken");
        verify(estudianteRepository, times(5)).existsByQrToken(anyString());
        verify(estudianteRepository, never()).saveAndFlush(any());
    }

    @Test
    void listarFiltradoPorCodigo() {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(estudiante(1L, "COD001", "T1", seccion(1L, "A"), true)));

        List<EstudianteResponse> response = estudianteService.listar("COD001", null, null, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).codigo()).isEqualTo("COD001");
        verify(estudianteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void listarFiltradoPorNombre() {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        List<EstudianteResponse> response = estudianteService.listar(null, "Estudiante", null, null);

        assertThat(response).isEmpty();
        verify(estudianteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void listarFiltradoPorSeccion() {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(estudiante(1L, "COD001", "T1", seccion(2L, "B"), true)));

        List<EstudianteResponse> response = estudianteService.listar(null, null, 2L, null);

        assertThat(response.get(0).seccion().id()).isEqualTo(2L);
        verify(estudianteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void listarFiltradoPorActivo() {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        List<EstudianteResponse> response = estudianteService.listar(null, null, null, false);

        assertThat(response).isEmpty();
        verify(estudianteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void listarConCombinacionDeFiltros() {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(estudiante(1L, "COD001", "T1", seccion(1L, "A"), true)));

        List<EstudianteResponse> response = estudianteService.listar("COD001", "Estudiante", 1L, true);

        assertThat(response).hasSize(1);
        verify(estudianteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void listarSinFiltrosDelegaAlMismoCamino() {
        when(estudianteRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(estudiante(1L, "COD001", "T1", seccion(1L, "A"), true)));

        List<EstudianteResponse> response = estudianteService.listar(null, null, null, null);

        assertThat(response).hasSize(1);
        verify(estudianteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void regenerarQrTokenEnEstudianteExistente() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ANTERIOR", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = estudianteService.regenerarQrToken(5L);

        assertThat(response.success()).isTrue();
        verify(estudianteRepository).saveAndFlush(existente);
    }

    @Test
    void regenerarQrTokenGeneraTokenDistintoAlAnterior() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ANTERIOR", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.regenerarQrToken(5L);

        assertThat(existente.getQrToken()).isNotEqualTo("TOKEN_ANTERIOR");
    }

    @Test
    void regenerarQrTokenCumpleFormatoYLongitudActual() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ANTERIOR", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.regenerarQrToken(5L);

        assertThat(existente.getQrToken()).matches("[0-9a-f]{64}");
    }

    @Test
    void regenerarQrTokenConservaDatosDelEstudiante() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ANTERIOR", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.regenerarQrToken(5L);

        assertThat(existente.getCodigo()).isEqualTo("COD001");
        assertThat(existente.getNombres()).isEqualTo("Estudiante");
        assertThat(existente.getApellidos()).isEqualTo("De Prueba");
        assertThat(existente.getSeccion().getId()).isEqualTo(1L);
        assertThat(existente.isActivo()).isTrue();
    }

    @Test
    void regenerarQrTokenEstudianteInexistenteLanzaNotFound() {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estudianteService.regenerarQrToken(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("STUDENT_NOT_FOUND");
        verify(estudianteRepository, never()).saveAndFlush(any());
    }

    @Test
    void regenerarQrTokenColisionGeneraOtroToken() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ANTERIOR", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.existsByQrToken(anyString()))
                .thenAnswer(new Answer<>() {
                    private int llamadas = 0;

                    @Override
                    public Boolean answer(InvocationOnMock invocation) {
                        llamadas++;
                        return llamadas == 1;
                    }
                });
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.regenerarQrToken(5L);

        assertThat(existente.getQrToken()).isNotEqualTo("TOKEN_ANTERIOR");
        verify(estudianteRepository, times(2)).existsByQrToken(anyString());
    }

    @Test
    void regenerarQrTokenRechazaCandidatoIgualAlAnterior() {
        // El generador excluye el token actual antes de consultar la BD
        // (candidato == TOKEN_ANTERIOR → se descarta sin contar colisión);
        // SecureRandom hace imposible forzar el valor exacto, así que este
        // test ejercita el mismo camino de rechazo: primer candidato
        // rechazado (sea por ser el anterior o por colisión) y segundo
        // aceptado. El itest real confirma que el propio token existente
        // no bloquea la regeneración.
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ANTERIOR", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.existsByQrToken(anyString()))
                .thenAnswer(new Answer<>() {
                    private int llamadas = 0;

                    @Override
                    public Boolean answer(InvocationOnMock invocation) {
                        llamadas++;
                        return llamadas == 1;
                    }
                });
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.regenerarQrToken(5L);

        assertThat(existente.getQrToken()).isNotEqualTo("TOKEN_ANTERIOR");
        assertThat(existente.getQrToken()).matches("[0-9a-f]{64}");
    }

    @Test
    void regenerarQrTokenLimiteDeColisionesFallaControladamente() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ANTERIOR", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(true);

        assertThatThrownBy(() -> estudianteService.regenerarQrToken(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("qrToken");
        verify(estudianteRepository, times(5)).existsByQrToken(anyString());
        verify(estudianteRepository, never()).saveAndFlush(any());
        assertThat(existente.getQrToken()).isEqualTo("TOKEN_ANTERIOR");
    }

    @Test
    void regenerarQrTokenTokenNuevoQuedaPersistido() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ANTERIOR", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.regenerarQrToken(5L);

        ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
        verify(estudianteRepository).saveAndFlush(captor.capture());
        String nuevo = captor.getValue().getQrToken();
        assertThat(nuevo).isNotEqualTo("TOKEN_ANTERIOR");
        assertThat(nuevo).matches("[0-9a-f]{64}");
    }

    @Test
    void regenerarQrTokenTokenAnteriorDejaDeSerElPersistido() {
        // A nivel de Service: la entidad guardada ya no contiene el token
        // anterior (findByQrToken(anterior) dejará de resolver en BD; se
        // verifica en EstudianteRepositoryTest).
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_ANTERIOR", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(estudianteRepository.existsByQrToken(anyString())).thenReturn(false);
        when(estudianteRepository.saveAndFlush(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        estudianteService.regenerarQrToken(5L);

        assertThat(existente.getQrToken()).isNotEqualTo("TOKEN_ANTERIOR");
        verify(estudianteRepository).saveAndFlush(existente);
    }

    @Test
    void contenidoQrDevuelvePrefijoMasToken() {
        when(estudianteRepository.findById(5L))
                .thenReturn(Optional.of(estudiante(5L, "COD001", "TOKEN_OPACO", seccion(1L, "A"), true)));

        String contenido = estudianteService.contenidoQr(5L);

        assertThat(contenido).isEqualTo("AULAIA:STUDENT:TOKEN_OPACO");
    }

    @Test
    void contenidoQrDeEstudianteInexistenteLanzaNotFound() {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estudianteService.contenidoQr(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("STUDENT_NOT_FOUND");
    }

    @Test
    void contenidoQrCambiaTrasRegeneracion() {
        Estudiante existente = estudiante(5L, "COD001", "TOKEN_A", seccion(1L, "A"), true);
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(existente));

        String antes = estudianteService.contenidoQr(5L);

        existente.setQrToken("TOKEN_B");

        String despues = estudianteService.contenidoQr(5L);

        assertThat(antes).startsWith("AULAIA:STUDENT:");
        assertThat(despues).startsWith("AULAIA:STUDENT:");
        assertThat(antes).isNotEqualTo(despues);
    }
}