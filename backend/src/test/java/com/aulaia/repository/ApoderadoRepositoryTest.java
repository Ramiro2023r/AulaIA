package com.aulaia.repository;

import com.aulaia.entity.Apoderado;
import com.aulaia.entity.Estudiante;
import com.aulaia.entity.EstudianteApoderado;
import com.aulaia.entity.Parentesco;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@SpringBootTest
@Transactional
@ActiveProfiles("itest")
@EnabledIfSystemProperty(named = "aulaia.itest", matches = "true")
class ApoderadoRepositoryTest {

    @Autowired
    private ApoderadoRepository apoderadoRepository;

    @Autowired
    private EstudianteApoderadoRepository estudianteApoderadoRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    private Estudiante estudiantePrueba;

    @BeforeEach
    void setUp() {
        // Obtenemos un estudiante existente cargado por las migraciones/fixtures de test
        // Asumiendo que el script V1 o tests base insertan al menos uno.
        estudiantePrueba = estudianteRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay estudiantes en la BD para probar"));
    }

    @Test
    void test1_CrearApoderado() {
        Apoderado apoderado = new Apoderado();
        apoderado.setNombres("Juan");
        apoderado.setApellidos("Pérez");
        apoderado.setTelefono("999888777");

        Apoderado guardado = apoderadoRepository.saveAndFlush(apoderado);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getCreatedAt()).isNotNull();
        assertThat(guardado.getUpdatedAt()).isNotNull();
        assertThat(guardado.isActivo()).isTrue();
    }

    @Test
    void test2_y_4_y_5_RelacionarApoderadoEstudiante_ConParentesco_Y_Principal() {
        Apoderado madre = new Apoderado();
        madre.setNombres("Ana");
        madre.setApellidos("Gómez");
        apoderadoRepository.saveAndFlush(madre);

        EstudianteApoderado relacion = new EstudianteApoderado();
        relacion.setEstudiante(estudiantePrueba);
        relacion.setApoderado(madre);
        relacion.setParentesco(Parentesco.MADRE);
        relacion.setPrincipal(true);

        EstudianteApoderado guardada = estudianteApoderadoRepository.saveAndFlush(relacion);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getParentesco()).isEqualTo(Parentesco.MADRE);
        assertThat(guardada.isPrincipal()).isTrue();

        List<EstudianteApoderado> relaciones = estudianteApoderadoRepository.findByEstudianteId(estudiantePrueba.getId());
        assertThat(relaciones).hasSize(1);
    }

    @Test
    void test3_RelacionarDosApoderadosMismoEstudiante() {
        Apoderado madre = new Apoderado();
        madre.setNombres("Ana");
        madre.setApellidos("Gómez");
        apoderadoRepository.saveAndFlush(madre);

        Apoderado padre = new Apoderado();
        padre.setNombres("Carlos");
        padre.setApellidos("Pérez");
        apoderadoRepository.saveAndFlush(padre);

        EstudianteApoderado rel1 = new EstudianteApoderado();
        rel1.setEstudiante(estudiantePrueba);
        rel1.setApoderado(madre);
        rel1.setParentesco(Parentesco.MADRE);
        rel1.setPrincipal(true);
        estudianteApoderadoRepository.saveAndFlush(rel1);

        EstudianteApoderado rel2 = new EstudianteApoderado();
        rel2.setEstudiante(estudiantePrueba);
        rel2.setApoderado(padre);
        rel2.setParentesco(Parentesco.PADRE);
        rel2.setPrincipal(false);
        estudianteApoderadoRepository.saveAndFlush(rel2);

        List<EstudianteApoderado> relaciones = estudianteApoderadoRepository.findByEstudianteId(estudiantePrueba.getId());
        assertThat(relaciones).hasSize(2);
    }

    @Test
    void test6_y_7_EstudianteInexistente_IntegridadForanea() {
        Apoderado tutor = new Apoderado();
        tutor.setNombres("Luis");
        tutor.setApellidos("Ramírez");
        apoderadoRepository.saveAndFlush(tutor);

        Estudiante estudianteFalso = new Estudiante();
        estudianteFalso.setId(99999L); // No existe en BD

        EstudianteApoderado relacion = new EstudianteApoderado();
        relacion.setEstudiante(estudianteFalso);
        relacion.setApoderado(tutor);
        relacion.setParentesco(Parentesco.TUTOR);

        // Debe lanzar DataIntegrityViolationException por FK de estudiante
        assertThatThrownBy(() -> estudianteApoderadoRepository.saveAndFlush(relacion))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
