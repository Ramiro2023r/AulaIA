package com.aulaia.repository;

import com.aulaia.entity.Grado;
import com.aulaia.entity.Seccion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración de {@link SeccionRepository} contra PostgreSQL real
 * (perfil {@code itest}), siguiendo el patrón de
 * {@code GradoRepositoryTest}.
 *
 * <p>Deshabilitadas por defecto. Ejecución explícita (PowerShell):
 * <pre>
 * $env:DB_HOST="localhost"; $env:DB_PORT="5433"; $env:DB_NAME="aulaia_db"
 * $env:DB_USERNAME="aulaia_user"; $env:DB_PASSWORD="TU_PASSWORD_LOCAL"
 * .\mvnw.cmd test "-Dtest=SeccionRepositoryTest" "-Daulaia.itest=true"
 * </pre>
 *
 * <p>Transaccionales: cada test revierte sus cambios. Verifican la FK a
 * grados y la UNIQUE física uq_seccion_grado_periodo (04-BD §6.4).
 */
@SpringBootTest
@Transactional
@ActiveProfiles("itest")
@EnabledIfSystemProperty(named = "aulaia.itest", matches = "true")
class SeccionRepositoryTest {

    @Autowired
    private SeccionRepository seccionRepository;

    @Autowired
    private GradoRepository gradoRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    private Grado guardarGrado(String nombre) {
        Grado grado = new Grado();
        grado.setNombre(nombre);
        return gradoRepository.saveAndFlush(grado);
    }

    private Seccion seccion(Grado grado, String nombre, String periodo) {
        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setNombre(nombre);
        seccion.setPeriodoAcademico(periodo);
        return seccion;
    }

    @Test
    void guardarSeccion() {
        Grado grado = guardarGrado("5.º Primaria");

        Seccion guardada = seccionRepository.saveAndFlush(seccion(grado, "A", "2026"));

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getGrado().getId()).isEqualTo(grado.getId());
        assertThat(guardada.getNombre()).isEqualTo("A");
        assertThat(guardada.getPeriodoAcademico()).isEqualTo("2026");
        assertThat(guardada.isActivo()).isTrue();
        assertThat(guardada.getCreatedAt()).isNotNull();
        assertThat(guardada.getUpdatedAt()).isNotNull();
    }

    @Test
    void buscarPorId() {
        Grado grado = guardarGrado("6.º Primaria");
        Seccion guardada = seccionRepository.saveAndFlush(seccion(grado, "B", "2027"));

        Optional<Seccion> encontrada = seccionRepository.findById(guardada.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getNombre()).isEqualTo("B");
        assertThat(encontrada.get().getGrado().getNombre()).isEqualTo("6.º Primaria");
    }

    @Test
    void fkRechazaGradoInexistente() {
        Grado gradoInexistente = new Grado();
        gradoInexistente.setId(999999L);

        assertThatThrownBy(() -> seccionRepository.saveAndFlush(seccion(gradoInexistente, "A", "2026")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueGradoNombrePeriodoImpideDuplicados() {
        Grado grado = guardarGrado("6.º Primaria");
        seccionRepository.saveAndFlush(seccion(grado, "A", "2026"));

        assertThatThrownBy(() -> seccionRepository.saveAndFlush(seccion(grado, "A", "2026")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_seccion_grado_periodo");
    }

    @Test
    void mismoNombreDiferenteGradoPermitido() {
        Grado grado5 = guardarGrado("5.º Primaria");
        Grado grado6 = guardarGrado("6.º Primaria");

        Seccion en5 = seccionRepository.saveAndFlush(seccion(grado5, "A", "2026"));
        Seccion en6 = seccionRepository.saveAndFlush(seccion(grado6, "A", "2026"));

        assertThat(en5.getId()).isNotNull();
        assertThat(en6.getId()).isNotNull();
        assertThat(en5.getGrado().getId()).isNotEqualTo(en6.getGrado().getId());
    }

    @Test
    void mismoNombreMismoGradoDiferentePeriodoPermitido() {
        Grado grado = guardarGrado("6.º Primaria");

        Seccion periodo2026 = seccionRepository.saveAndFlush(seccion(grado, "A", "2026"));
        Seccion periodo2027 = seccionRepository.saveAndFlush(seccion(grado, "A", "2027"));

        assertThat(periodo2026.getId()).isNotNull();
        assertThat(periodo2027.getId()).isNotNull();
        assertThat(periodo2026.getPeriodoAcademico()).isNotEqualTo(periodo2027.getPeriodoAcademico());
    }

    @Test
    void uniqueEsCaseSensitiveYPermiteNombreAYMinuscula() {
        Grado grado = guardarGrado("6.º Primaria");

        Seccion mayuscula = seccionRepository.saveAndFlush(seccion(grado, "A", "2026"));
        Seccion minuscula = seccionRepository.saveAndFlush(seccion(grado, "a", "2026"));

        assertThat(mayuscula.getId()).isNotNull();
        assertThat(minuscula.getId()).isNotNull();
        assertThat(mayuscula.getNombre()).isNotEqualTo(minuscula.getNombre());
        assertThat(gradoRepository.findAll()).hasSize(1);
    }

    @Test
    void listarOrdenaPorIdAsc() {
        Grado grado = guardarGrado("6.º Primaria");
        Seccion primero = seccionRepository.saveAndFlush(seccion(grado, "A", "2026"));
        Seccion segundo = seccionRepository.saveAndFlush(seccion(grado, "B", "2026"));

        List<Seccion> lista = seccionRepository.findAllByOrderByIdAsc();

        assertThat(lista).extracting(Seccion::getId)
                .containsSubsequence(primero.getId(), segundo.getId());
    }
}