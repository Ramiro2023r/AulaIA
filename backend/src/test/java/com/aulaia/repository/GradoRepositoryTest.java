package com.aulaia.repository;

import com.aulaia.entity.Grado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración de {@link GradoRepository} contra PostgreSQL real
 * (perfil {@code itest}), siguiendo el patrón de
 * {@code UsuarioRepositoryTest}.
 *
 * <p>Deshabilitadas por defecto. Ejecución explícita (PowerShell):
 * <pre>
 * $env:DB_HOST="localhost"; $env:DB_PORT="5433"; $env:DB_NAME="aulaia_db"
 * $env:DB_USERNAME="aulaia_user"; $env:DB_PASSWORD="TU_PASSWORD_LOCAL"
 * .\mvnw.cmd test "-Dtest=GradoRepositoryTest" "-Daulaia.itest=true"
 * </pre>
 *
 * <p>Transaccionales: cada test revierte sus cambios.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("itest")
@EnabledIfSystemProperty(named = "aulaia.itest", matches = "true")
class GradoRepositoryTest {

    @Autowired
    private GradoRepository gradoRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    @Test
    void guardarGrado() {
        Grado guardado = gradoRepository.saveAndFlush(grado("5.º Primaria", null, 1));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getNombre()).isEqualTo("5.º Primaria");
        assertThat(guardado.getNivel()).isEqualTo("PRIMARIA");
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getCreatedAt()).isNotNull();
    }

    @Test
    void buscarPorId() {
        Grado guardado = gradoRepository.saveAndFlush(grado("6.º Primaria", "PRIMARIA", 2));

        Optional<Grado> encontrado = gradoRepository.findById(guardado.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNivel()).isEqualTo("PRIMARIA");
        assertThat(encontrado.get().getOrden()).isEqualTo(2);
    }

    @Test
    void guardarDosGradosConElMismoNombre() {
        Grado primero = gradoRepository.saveAndFlush(grado("1.º Primaria", null, null));
        Grado segundo = gradoRepository.saveAndFlush(grado("1.º Primaria", null, null));

        assertThat(primero.getId()).isNotNull();
        assertThat(segundo.getId()).isNotNull();
        assertThat(primero.getId()).isNotEqualTo(segundo.getId());
        assertThat(gradoRepository.findAll()).hasSize(2);
    }

    @Test
    void listarOrdenaPorIdAsc() {
        Grado primero = gradoRepository.saveAndFlush(grado("B", null, null));
        Grado segundo = gradoRepository.saveAndFlush(grado("A", null, null));

        List<Grado> lista = gradoRepository.findAllByOrderByIdAsc();

        assertThat(lista).extracting(Grado::getId)
                .containsSubsequence(primero.getId(), segundo.getId());
    }

    private Grado grado(String nombre, String nivel, Integer orden) {
        Grado grado = new Grado();
        grado.setNombre(nombre);
        if (nivel != null) {
            grado.setNivel(nivel);
        }
        grado.setOrden(orden);
        return grado;
    }
}