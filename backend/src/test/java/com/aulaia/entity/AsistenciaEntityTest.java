package com.aulaia.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de la entidad {@link Asistencia} y sus enums
 * (04-BD §8.1, 07-PLAN Prompt 7.1 §26).
 *
 * <p>No requieren Spring ni base de datos.
 */
class AsistenciaEntityTest {

    // ─── Enum EstadoAsistencia ────────────────────────────────────────────

    @Test
    void estadoAsistenciaTieneExactamenteCuatroValores() {
        assertThat(EstadoAsistencia.values()).hasSize(4);
    }

    @Test
    void estadoAsistenciaContieneValoresExactos() {
        EstadoAsistencia[] valores = EstadoAsistencia.values();
        assertThat(valores).containsExactlyInAnyOrder(
                EstadoAsistencia.PRESENTE,
                EstadoAsistencia.TARDANZA,
                EstadoAsistencia.AUSENTE,
                EstadoAsistencia.JUSTIFICADO
        );
    }

    @Test
    void estadoAsistenciaNombresCoincidenConBD() {
        assertThat(EstadoAsistencia.PRESENTE.name()).isEqualTo("PRESENTE");
        assertThat(EstadoAsistencia.TARDANZA.name()).isEqualTo("TARDANZA");
        assertThat(EstadoAsistencia.AUSENTE.name()).isEqualTo("AUSENTE");
        assertThat(EstadoAsistencia.JUSTIFICADO.name()).isEqualTo("JUSTIFICADO");
    }

    // ─── Enum MetodoRegistro ──────────────────────────────────────────────

    @Test
    void metodoRegistroTieneExactamenteCuatroValores() {
        assertThat(MetodoRegistro.values()).hasSize(4);
    }

    @Test
    void metodoRegistroContieneValoresExactos() {
        MetodoRegistro[] valores = MetodoRegistro.values();
        assertThat(valores).containsExactlyInAnyOrder(
                MetodoRegistro.QR,
                MetodoRegistro.CODIGO,
                MetodoRegistro.MANUAL_DOCENTE,
                MetodoRegistro.SISTEMA
        );
    }

    @Test
    void metodoRegistroNombresCoincidenConBD() {
        assertThat(MetodoRegistro.QR.name()).isEqualTo("QR");
        assertThat(MetodoRegistro.CODIGO.name()).isEqualTo("CODIGO");
        assertThat(MetodoRegistro.MANUAL_DOCENTE.name()).isEqualTo("MANUAL_DOCENTE");
        assertThat(MetodoRegistro.SISTEMA.name()).isEqualTo("SISTEMA");
    }

    // ─── Entidad Asistencia ───────────────────────────────────────────────

    @Test
    void asistenciaEsInstanciable() {
        Asistencia asistencia = new Asistencia();
        assertThat(asistencia).isNotNull();
    }

    @Test
    void asistenciaIdInicialmenteNulo() {
        Asistencia asistencia = new Asistencia();
        assertThat(asistencia.getId()).isNull();
    }

    @Test
    void asistenciaSesionClaseGetterSetter() {
        SesionClase sesion = new SesionClase();
        Asistencia asistencia = new Asistencia();
        asistencia.setSesionClase(sesion);
        assertThat(asistencia.getSesionClase()).isSameAs(sesion);
    }

    @Test
    void asistenciaEstudianteGetterSetter() {
        Estudiante estudiante = new Estudiante();
        Asistencia asistencia = new Asistencia();
        asistencia.setEstudiante(estudiante);
        assertThat(asistencia.getEstudiante()).isSameAs(estudiante);
    }

    @Test
    void asistenciaFechaHoraGetterSetter() {
        OffsetDateTime ahora = OffsetDateTime.now();
        Asistencia asistencia = new Asistencia();
        asistencia.setFechaHora(ahora);
        assertThat(asistencia.getFechaHora()).isEqualTo(ahora);
    }

    @Test
    void asistenciaEstadoGetterSetter() {
        Asistencia asistencia = new Asistencia();
        asistencia.setEstado(EstadoAsistencia.PRESENTE);
        assertThat(asistencia.getEstado()).isEqualTo(EstadoAsistencia.PRESENTE);
    }

    @Test
    void asistenciaMetodoGetterSetter() {
        Asistencia asistencia = new Asistencia();
        asistencia.setMetodo(MetodoRegistro.QR);
        assertThat(asistencia.getMetodo()).isEqualTo(MetodoRegistro.QR);
    }

    @Test
    void asistenciaObservacionGetterSetter() {
        Asistencia asistencia = new Asistencia();
        asistencia.setObservacion("Llegó tarde por lluvia");
        assertThat(asistencia.getObservacion()).isEqualTo("Llegó tarde por lluvia");
    }

    @Test
    void asistenciaObservacionPuedeSerNull() {
        Asistencia asistencia = new Asistencia();
        asistencia.setObservacion(null);
        assertThat(asistencia.getObservacion()).isNull();
    }

    @Test
    void asistenciaCreatedAtGetterSetter() {
        OffsetDateTime ahora = OffsetDateTime.now();
        Asistencia asistencia = new Asistencia();
        asistencia.setCreatedAt(ahora);
        assertThat(asistencia.getCreatedAt()).isEqualTo(ahora);
    }

    @Test
    void asistenciaUpdatedAtGetterSetter() {
        OffsetDateTime ahora = OffsetDateTime.now();
        Asistencia asistencia = new Asistencia();
        asistencia.setUpdatedAt(ahora);
        assertThat(asistencia.getUpdatedAt()).isEqualTo(ahora);
    }

    @Test
    void toStringNoLanzaExcepcion() {
        Asistencia asistencia = new Asistencia();
        asistencia.setEstado(EstadoAsistencia.TARDANZA);
        asistencia.setMetodo(MetodoRegistro.CODIGO);
        asistencia.setFechaHora(OffsetDateTime.now());
        String resultado = asistencia.toString();
        assertThat(resultado).contains("Asistencia");
        assertThat(resultado).doesNotContain("sesion"); // no debe acceder a LAZY
        assertThat(resultado).doesNotContain("estudiante");
    }
}
