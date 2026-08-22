package com.aulaia.repository;

import com.aulaia.entity.Estudiante;
import com.aulaia.entity.Seccion;
import com.aulaia.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración del repositorio de Estudiantes con Testcontainers.
 * <p>
 * Verifica que las operaciones de BD funcionan contra PostgreSQL real.
 */
@DataJpaTest
@ActiveProfiles("testcontainers")
class EstudianteRepositoryIT extends TestcontainersConfig {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EstudianteRepository repository;

    @Test
    void shouldSaveAndFindEstudiante() {
        // Given
        Seccion seccion = new Seccion();
        seccion.setNombre("A");
        seccion.setPeriodoAcademico("2026");
        entityManager.persist(seccion);

        Estudiante estudiante = new Estudiante();
        estudiante.setCodigo("TEST-IT-001");
        estudiante.setQrToken("AULAIA:STUDENT:IT001");
        estudiante.setNombres("Test");
        estudiante.setApellidos("Integration");
        estudiante.setSeccion(seccion);
        estudiante.setActivo(true);
        entityManager.persistAndFlush(estudiante);

        // When
        var found = repository.findByCodigo("TEST-IT-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNombres()).isEqualTo("Test");
        assertThat(found.get().getApellidos()).isEqualTo("Integration");
        assertThat(found.get().getSeccion().getId()).isEqualTo(seccion.getId());
    }

    @Test
    void shouldFindByQrToken() {
        // Given
        Seccion seccion = new Seccion();
        seccion.setNombre("B");
        seccion.setPeriodoAcademico("2026");
        entityManager.persist(seccion);

        Estudiante estudiante = new Estudiante();
        estudiante.setCodigo("TEST-IT-002");
        estudiante.setQrToken("AULAIA:STUDENT:IT002");
        estudiante.setNombres("QR");
        estudiante.setApellidos("Token");
        estudiante.setSeccion(seccion);
        estudiante.setActivo(true);
        entityManager.persistAndFlush(estudiante);

        // When
        var found = repository.findByQrToken("AULAIA:STUDENT:IT002");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCodigo()).isEqualTo("TEST-IT-002");
    }

    @Test
    void shouldFindBySeccion() {
        // Given
        Seccion seccion = new Seccion();
        seccion.setNombre("C");
        seccion.setPeriodoAcademico("2026");
        entityManager.persist(seccion);

        Estudiante e1 = new Estudiante();
        e1.setCodigo("TEST-IT-003");
        e1.setQrToken("AULAIA:STUDENT:IT003");
        e1.setNombres("Uno");
        e1.setApellidos("Test");
        e1.setSeccion(seccion);
        e1.setActivo(true);
        entityManager.persist(e1);

        Estudiante e2 = new Estudiante();
        e2.setCodigo("TEST-IT-004");
        e2.setQrToken("AULAIA:STUDENT:IT004");
        e2.setNombres("Dos");
        e2.setApellidos("Test");
        e2.setSeccion(seccion);
        e2.setActivo(true);
        entityManager.persist(e2);
        entityManager.flush();

        // When
        var found = repository.findBySeccionIdAndActivoTrue(seccion.getId());

        // Then
        assertThat(found).hasSize(2);
    }
}