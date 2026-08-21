package com.aulaia.repository;

import com.aulaia.entity.Estudiante;
import com.aulaia.entity.Grado;
import com.aulaia.entity.Seccion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración de {@link EstudianteRepository} contra PostgreSQL
 * real (perfil {@code itest}), siguiendo el patrón de
 * {@code SeccionRepositoryTest}.
 *
 * <p>Deshabilitadas por defecto. Ejecución explícita (PowerShell):
 * <pre>
 * $env:DB_HOST="localhost"; $env:DB_PORT="5433"; $env:DB_NAME="aulaia_db"
 * $env:DB_USERNAME="aulaia_user"; $env:DB_PASSWORD="TU_PASSWORD_LOCAL"
 * .\mvnw.cmd test "-Dtest=EstudianteRepositoryTest" "-Daulaia.itest=true"
 * </pre>
 *
 * <p>Transaccionales: cada test revierte sus cambios. Verifican las
 * restricciones físicas de {@code estudiantes} (04-BD §6.5): UNIQUE de
 * codigo, UNIQUE de qr_token y FK a secciones. Datos ficticios, nunca de
 * menores reales.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("itest")
@EnabledIfSystemProperty(named = "aulaia.itest", matches = "true")
class EstudianteRepositoryTest {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private SeccionRepository seccionRepository;

    @Autowired
    private GradoRepository gradoRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aulaia.repository.JustificacionRepository justificacionRepository;

    private Seccion guardarSeccion(String nombreSeccion) {
        Grado grado = new Grado();
        grado.setNombre("6.º Primaria");
        grado = gradoRepository.saveAndFlush(grado);
        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setNombre(nombreSeccion);
        seccion.setPeriodoAcademico("2026");
        return seccionRepository.saveAndFlush(seccion);
    }

    private Estudiante estudiante(String codigo, String qrToken, Seccion seccion) {
        Estudiante estudiante = new Estudiante();
        estudiante.setCodigo(codigo);
        estudiante.setQrToken(qrToken);
        estudiante.setNombres("Estudiante");
        estudiante.setApellidos("De Prueba");
        estudiante.setSeccion(seccion);
        return estudiante;
    }

    @Test
    void guardarEstudianteCorrectamente() {
        Seccion seccion = guardarSeccion("A");

        Estudiante guardado = estudianteRepository.saveAndFlush(estudiante("COD001", "TOKEN_A", seccion));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getCodigo()).isEqualTo("COD001");
        assertThat(guardado.getQrToken()).isEqualTo("TOKEN_A");
        assertThat(guardado.getNombres()).isEqualTo("Estudiante");
        assertThat(guardado.getApellidos()).isEqualTo("De Prueba");
        assertThat(guardado.getSeccion().getId()).isEqualTo(seccion.getId());
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getCreatedAt()).isNotNull();
        assertThat(guardado.getUpdatedAt()).isNotNull();
    }

    @Test
    void buscarPorId() {
        Seccion seccion = guardarSeccion("B");
        Estudiante guardado = estudianteRepository.saveAndFlush(estudiante("COD002", "TOKEN_B", seccion));

        Optional<Estudiante> encontrado = estudianteRepository.findById(guardado.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCodigo()).isEqualTo("COD002");
        assertThat(encontrado.get().getSeccion().getNombre()).isEqualTo("B");
    }

    @Test
    void buscarPorCodigo() {
        Seccion seccion = guardarSeccion("A");
        estudianteRepository.saveAndFlush(estudiante("COD003", "TOKEN_C", seccion));

        Optional<Estudiante> encontrado = estudianteRepository.findByCodigo("COD003");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getQrToken()).isEqualTo("TOKEN_C");
    }

    @Test
    void buscarPorQrToken() {
        Seccion seccion = guardarSeccion("A");
        estudianteRepository.saveAndFlush(estudiante("COD004", "TOKEN_D", seccion));

        Optional<Estudiante> encontrado = estudianteRepository.findByQrToken("TOKEN_D");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCodigo()).isEqualTo("COD004");
    }

    @Test
    void codigoDuplicadoRechazadoPorUnique() {
        Seccion seccion = guardarSeccion("A");
        estudianteRepository.saveAndFlush(estudiante("COD001", "TOKEN_A", seccion));

        assertThatThrownBy(() -> estudianteRepository.saveAndFlush(estudiante("COD001", "TOKEN_B", seccion)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("estudiantes_codigo_key");
    }

    @Test
    void qrTokenDuplicadoRechazadoPorUnique() {
        Seccion seccion = guardarSeccion("A");
        estudianteRepository.saveAndFlush(estudiante("COD001", "TOKEN_X", seccion));

        assertThatThrownBy(() -> estudianteRepository.saveAndFlush(estudiante("COD002", "TOKEN_X", seccion)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("estudiantes_qr_token_key");
    }

    @Test
    void fkRechazaSeccionInexistente() {
        Seccion seccionInexistente = new Seccion();
        seccionInexistente.setId(999999L);

        assertThatThrownBy(() -> estudianteRepository.saveAndFlush(estudiante("COD010", "TOKEN_Z", seccionInexistente)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByCodigo() {
        Seccion seccion = guardarSeccion("A");
        estudianteRepository.saveAndFlush(estudiante("COD005", "TOKEN_E", seccion));

        assertThat(estudianteRepository.existsByCodigo("COD005")).isTrue();
        assertThat(estudianteRepository.existsByCodigo("NO_EXISTE")).isFalse();
    }

    @Test
    void existsByQrToken() {
        Seccion seccion = guardarSeccion("A");
        estudianteRepository.saveAndFlush(estudiante("COD006", "TOKEN_F", seccion));

        assertThat(estudianteRepository.existsByQrToken("TOKEN_F")).isTrue();
        assertThat(estudianteRepository.existsByQrToken("NO_EXISTE")).isFalse();
    }

    @Test
    void filtroCodigoExacto() {
        Seccion seccion = guardarSeccion("A");
        estudianteRepository.saveAndFlush(estudiante("COD020", "TOKEN_G", seccion));

        Specification<Estudiante> spec = (root, query, cb) -> cb.equal(root.get("codigo"), "COD020");

        List<Estudiante> resultado = estudianteRepository.findAll(spec, Sort.by("id"));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("COD020");
    }

    @Test
    void filtroCodigoExactoNoCoincideConParcial() {
        Seccion seccion = guardarSeccion("A");
        estudianteRepository.saveAndFlush(estudiante("COD021", "TOKEN_H", seccion));

        Specification<Estudiante> spec = (root, query, cb) -> cb.equal(root.get("codigo"), "COD02");

        List<Estudiante> resultado = estudianteRepository.findAll(spec, Sort.by("id"));

        assertThat(resultado).isEmpty();
    }

    @Test
    void filtroNombreCoincidenciaParcialSobreNombres() {
        Seccion seccion = guardarSeccion("A");
        estudianteRepository.saveAndFlush(estudiante("COD022", "TOKEN_I", seccion));

        Specification<Estudiante> spec = (root, query, cb) -> cb.like(root.get("nombres"), "%Estudiante%");

        List<Estudiante> resultado = estudianteRepository.findAll(spec, Sort.by("id"));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombres()).isEqualTo("Estudiante");
    }

    @Test
    void filtroNombreNoBuscaSobreApellidos() {
        Seccion seccion = guardarSeccion("A");
        estudianteRepository.saveAndFlush(estudiante("COD023", "TOKEN_J", seccion));

        Specification<Estudiante> spec = (root, query, cb) -> cb.like(root.get("nombres"), "%Prueba%");

        List<Estudiante> resultado = estudianteRepository.findAll(spec, Sort.by("id"));

        assertThat(resultado).isEmpty();
    }

    @Test
    void filtroPorSeccion() {
        Seccion seccionA = guardarSeccion("A");
        Seccion seccionB = guardarSeccion("B");
        estudianteRepository.saveAndFlush(estudiante("COD024", "TOKEN_K", seccionA));
        estudianteRepository.saveAndFlush(estudiante("COD025", "TOKEN_L", seccionB));

        Specification<Estudiante> spec = (root, query, cb) -> cb.equal(root.get("seccion").get("id"), seccionA.getId());

        List<Estudiante> resultado = estudianteRepository.findAll(spec, Sort.by("id"));

        assertThat(resultado).extracting(Estudiante::getCodigo).containsExactly("COD024");
    }

    @Test
    void filtroPorActivo() {
        Seccion seccion = guardarSeccion("A");
        Estudiante activo = estudianteRepository.saveAndFlush(estudiante("COD026", "TOKEN_M", seccion));
        Estudiante inactivo = estudianteRepository.saveAndFlush(estudiante("COD027", "TOKEN_N", seccion));
        inactivo.setActivo(false);
        estudianteRepository.saveAndFlush(inactivo);

        Specification<Estudiante> spec = (root, query, cb) -> cb.equal(root.get("activo"), true);

        List<Estudiante> resultado = estudianteRepository.findAll(spec, Sort.by("id"));

        assertThat(resultado).extracting(Estudiante::getCodigo).containsExactly("COD026");
        assertThat(activo.getId()).isNotNull();
    }

    @Test
    void tokenAnteriorDejaDeResolverYTokenNuevoResuelve() {
        Seccion seccion = guardarSeccion("A");
        Estudiante guardado = estudianteRepository.saveAndFlush(estudiante("COD030", "TOKEN_VIEJO", seccion));

        assertThat(estudianteRepository.findByQrToken("TOKEN_VIEJO")).isPresent();

        guardado.setQrToken("TOKEN_NUEVO");
        estudianteRepository.saveAndFlush(guardado);

        assertThat(estudianteRepository.findByQrToken("TOKEN_VIEJO")).isEmpty();
        assertThat(estudianteRepository.findByQrToken("TOKEN_NUEVO"))
                .isPresent()
                .get()
                .extracting(Estudiante::getCodigo)
                .isEqualTo("COD030");
    }

    @Test
    void combinacionDeFiltrosAnd() {
        Seccion seccionA = guardarSeccion("A");
        Seccion seccionB = guardarSeccion("B");
        estudianteRepository.saveAndFlush(estudiante("COD028", "TOKEN_O", seccionA));
        estudianteRepository.saveAndFlush(estudiante("COD029", "TOKEN_P", seccionB));

        Specification<Estudiante> spec = Specification
                .<Estudiante>where((root, query, cb) -> cb.equal(root.get("codigo"), "COD028"))
                .and((root, query, cb) -> cb.equal(root.get("seccion").get("id"), seccionA.getId()))
                .and((root, query, cb) -> cb.equal(root.get("activo"), true));

        List<Estudiante> resultado = estudianteRepository.findAll(spec, Sort.by("id"));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("COD028");
    }
}