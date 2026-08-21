package com.aulaia.repository;

import com.aulaia.entity.Curso;
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
 * Pruebas de integración de {@link CursoRepository} contra PostgreSQL real
 * (perfil {@code itest}), siguiendo el patrón de
 * {@code SeccionRepositoryTest}.
 *
 * <p>Deshabilitadas por defecto. Ejecución explícita (PowerShell):
 * <pre>
 * $env:DB_HOST="localhost"; $env:DB_PORT="5433"; $env:DB_NAME="aulaia_db"
 * $env:DB_USERNAME="aulaia_user"; $env:DB_PASSWORD="TU_PASSWORD_LOCAL"
 * .\mvnw.cmd test "-Dtest=CursoRepositoryTest" "-Daulaia.itest=true"
 * </pre>
 *
 * <p>Transaccionales: cada test revierte sus cambios. Verifican la
 * estructura real de {@code cursos} (04-BD §6.6): sin UNIQUE, la BD
 * permite dos cursos con el mismo nombre.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("itest")
@EnabledIfSystemProperty(named = "aulaia.itest", matches = "true")
class CursoRepositoryTest {

    @Autowired
    private CursoRepository cursoRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    private Curso curso(String nombre, String descripcion) {
        Curso curso = new Curso();
        curso.setNombre(nombre);
        curso.setDescripcion(descripcion);
        return curso;
    }

    @Test
    void guardarCurso() {
        Curso guardado = cursoRepository.saveAndFlush(curso("Computación", "Curso de ofimática"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getNombre()).isEqualTo("Computación");
        assertThat(guardado.getDescripcion()).isEqualTo("Curso de ofimática");
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getCreatedAt()).isNotNull();
        assertThat(guardado.getUpdatedAt()).isNotNull();
    }

    @Test
    void guardarCursoSinDescripcion() {
        Curso guardado = cursoRepository.saveAndFlush(curso("Matemáticas", null));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getDescripcion()).isNull();
    }

    @Test
    void buscarPorId() {
        Curso guardado = cursoRepository.saveAndFlush(curso("Computación", "Curso de ofimática"));

        Optional<Curso> encontrado = cursoRepository.findById(guardado.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNombre()).isEqualTo("Computación");
    }

    @Test
    void guardarDosCursosConElMismoNombrePermitido() {
        Curso primero = cursoRepository.saveAndFlush(curso("Computación", null));
        Curso segundo = cursoRepository.saveAndFlush(curso("Computación", null));

        assertThat(primero.getId()).isNotNull();
        assertThat(segundo.getId()).isNotNull();
        assertThat(primero.getNombre()).isEqualTo(segundo.getNombre());
        assertThat(cursoRepository.findAll()).hasSize(2);
    }

    @Test
    void listarOrdenaPorIdAsc() {
        Curso primero = cursoRepository.saveAndFlush(curso("Computación", null));
        Curso segundo = cursoRepository.saveAndFlush(curso("Matemáticas", null));

        List<Curso> lista = cursoRepository.findAllByOrderByIdAsc();

        assertThat(lista).extracting(Curso::getId)
                .containsSubsequence(primero.getId(), segundo.getId());
    }
}