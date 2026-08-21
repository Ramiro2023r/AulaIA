package com.aulaia.service;

import com.aulaia.entity.Estudiante;
import com.aulaia.entity.MetodoRegistro;
import com.aulaia.entity.Seccion;
import com.aulaia.exception.BusinessException;
import com.aulaia.exception.ResourceNotFoundException;
import com.aulaia.repository.EstudianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link EstudianteResolverService} (Prompt 7.2).
 *
 * <p>Cubre los cuatro casos documentados (07-PLAN 7.2):
 * <ol>
 *   <li>QR válido → estudiante resuelto.</li>
 *   <li>Formato QR inválido → {@link BusinessException} con código {@code INVALID_QR}.</li>
 *   <li>Código escolar válido → estudiante resuelto.</li>
 *   <li>Código escolar inexistente → {@link ResourceNotFoundException} con código
 *       {@code STUDENT_NOT_FOUND}.</li>
 * </ol>
 *
 * <p>No dependen de PostgreSQL. Datos ficticios; ningún nombre real de menor.
 */
class EstudianteResolverServiceTest {

    private EstudianteRepository estudianteRepository;
    private EstudianteResolverService resolver;

    @BeforeEach
    void setUp() {
        estudianteRepository = mock(EstudianteRepository.class);
        resolver = new EstudianteResolverService(estudianteRepository);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Estudiante estudianteConId(Long id, String codigo, String qrToken) {
        Seccion seccion = new Seccion();
        seccion.setId(1L);
        seccion.setNombre("A");

        Estudiante e = new Estudiante();
        e.setId(id);
        e.setCodigo(codigo);
        e.setQrToken(qrToken);
        e.setNombres("Juan");
        e.setApellidos("Pérez");
        e.setSeccion(seccion);
        e.setActivo(true);
        return e;
    }

    // =========================================================================
    // Casos: método QR
    // =========================================================================

    /**
     * QR VÁLIDO — el contenido comienza con AULAIA:STUDENT: y el token existe en BD.
     */
    @Test
    void qrValido_devuelveEstudiante() {
        String token = "abc123tokenSeguro";
        String contenidoQr = EstudianteResolverService.PREFIJO_QR + token;
        Estudiante esperado = estudianteConId(1L, "COD001", token);

        when(estudianteRepository.findByQrToken(token)).thenReturn(Optional.of(esperado));

        Estudiante resultado = resolver.resolver(MetodoRegistro.QR, contenidoQr);

        assertThat(resultado).isSameAs(esperado);
        verify(estudianteRepository).findByQrToken(token);
    }

    /**
     * FORMATO QR INVÁLIDO — contenido no comienza con AULAIA:STUDENT:.
     */
    @Test
    void qrConFormatoIncorrecto_lanzaBusinessExceptionInvalidQr() {
        String contenidoMalformado = "http://phishing.example.com";

        assertThatThrownBy(() -> resolver.resolver(MetodoRegistro.QR, contenidoMalformado))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(EstudianteResolverService.CODE_INVALID_QR));

        verifyNoInteractions(estudianteRepository);
    }

    /**
     * QR NULL — nulo se trata como formato inválido.
     */
    @Test
    void qrNull_lanzaBusinessExceptionInvalidQr() {
        assertThatThrownBy(() -> resolver.resolver(MetodoRegistro.QR, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(EstudianteResolverService.CODE_INVALID_QR));

        verifyNoInteractions(estudianteRepository);
    }

    /**
     * QR con prefijo correcto pero token vacío — formato inválido.
     */
    @Test
    void qrConPrefijoCorrectoYTokenVacio_lanzaBusinessExceptionInvalidQr() {
        String soloPrefix = EstudianteResolverService.PREFIJO_QR;

        assertThatThrownBy(() -> resolver.resolver(MetodoRegistro.QR, soloPrefix))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(EstudianteResolverService.CODE_INVALID_QR));

        verifyNoInteractions(estudianteRepository);
    }

    /**
     * QR válido pero token no existe en BD → STUDENT_NOT_FOUND.
     */
    @Test
    void qrValidoTokenNoExistente_lanzaResourceNotFound() {
        String token = "tokenQueNoExiste";
        String contenidoQr = EstudianteResolverService.PREFIJO_QR + token;

        when(estudianteRepository.findByQrToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolver(MetodoRegistro.QR, contenidoQr))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo(EstudianteResolverService.CODE_STUDENT_NOT_FOUND));

        verify(estudianteRepository).findByQrToken(token);
    }

    // =========================================================================
    // Casos: método CODIGO
    // =========================================================================

    /**
     * CÓDIGO VÁLIDO — estudiante existe en BD.
     */
    @Test
    void codigoValido_devuelveEstudiante() {
        String codigo = "EST2024001";
        Estudiante esperado = estudianteConId(2L, codigo, "token-seg");

        when(estudianteRepository.findByCodigo(codigo)).thenReturn(Optional.of(esperado));

        Estudiante resultado = resolver.resolver(MetodoRegistro.CODIGO, codigo);

        assertThat(resultado).isSameAs(esperado);
        verify(estudianteRepository).findByCodigo(codigo);
    }

    /**
     * CÓDIGO VÁLIDO con espacios — se recorta antes de buscar.
     */
    @Test
    void codigoConEspacios_seRecortaAntesDeResolver() {
        String codigoConEspacios = "  EST2024001  ";
        String codigoRecortado = "EST2024001";
        Estudiante esperado = estudianteConId(2L, codigoRecortado, "token-seg");

        when(estudianteRepository.findByCodigo(codigoRecortado)).thenReturn(Optional.of(esperado));

        Estudiante resultado = resolver.resolver(MetodoRegistro.CODIGO, codigoConEspacios);

        assertThat(resultado).isSameAs(esperado);
        verify(estudianteRepository).findByCodigo(codigoRecortado);
    }

    /**
     * CÓDIGO INEXISTENTE — no está en BD → STUDENT_NOT_FOUND.
     */
    @Test
    void codigoInexistente_lanzaResourceNotFound() {
        String codigo = "NOEXISTE999";

        when(estudianteRepository.findByCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolver(MetodoRegistro.CODIGO, codigo))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo(EstudianteResolverService.CODE_STUDENT_NOT_FOUND));

        verify(estudianteRepository).findByCodigo(codigo);
    }

    /**
     * CÓDIGO NULO — entrada inválida rápida (falla antes de ir a BD).
     */
    @Test
    void codigoNull_lanzaBusinessException() {
        assertThatThrownBy(() -> resolver.resolver(MetodoRegistro.CODIGO, null))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(estudianteRepository);
    }

    /**
     * CÓDIGO VACÍO (blank) — entrada inválida rápida.
     */
    @Test
    void codigoBlank_lanzaBusinessException() {
        assertThatThrownBy(() -> resolver.resolver(MetodoRegistro.CODIGO, "   "))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(estudianteRepository);
    }

    // =========================================================================
    // Caso: método no soportado
    // =========================================================================

    /**
     * Métodos MANUAL_DOCENTE y SISTEMA no son válidos en la resolución directa
     * de estudiante (requieren flujo diferente — 07-PLAN 7.2 solo documenta QR y CODIGO).
     */
    @Test
    void metodoManualDocente_lanzaIllegalArgument() {
        assertThatThrownBy(() -> resolver.resolver(MetodoRegistro.MANUAL_DOCENTE, "cualquier"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(estudianteRepository);
    }
}
