package com.aulaia.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del modelo {@link SesionClase} (Prompt 6.1, 07-PLAN;
 * 04-BD §7.1): obligatoriedad de horario/fecha/estado (NOT NULL físico),
 * nullability de horaApertura/horaCierre (TIMESTAMPTZ NULL) y los 4
 * estados válidos exactos. Usa el validador Bean Validation real.
 * No prueba transiciones de estado (pertenecen a prompts posteriores).
 */
class SesionClaseEntityTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    private SesionClase sesionValida() {
        SesionClase sesion = new SesionClase();
        sesion.setHorario(new Horario());
        sesion.setFecha(LocalDate.of(2026, 8, 19));
        sesion.setEstado(SesionClaseEstado.PROGRAMADA);
        return sesion;
    }

    private Set<?> violaciones(SesionClase sesion) {
        return validator.validate(sesion);
    }

    @Test
    void sesionProgramadaValida() {
        SesionClase sesion = sesionValida();
        sesion.setEstado(SesionClaseEstado.PROGRAMADA);
        assertThat(violaciones(sesion)).isEmpty();
    }

    @Test
    void sesionAbiertaValida() {
        SesionClase sesion = sesionValida();
        sesion.setEstado(SesionClaseEstado.ABIERTA);
        assertThat(violaciones(sesion)).isEmpty();
    }

    @Test
    void sesionCerradaValida() {
        SesionClase sesion = sesionValida();
        sesion.setEstado(SesionClaseEstado.CERRADA);
        assertThat(violaciones(sesion)).isEmpty();
    }

    @Test
    void sesionCanceladaValida() {
        SesionClase sesion = sesionValida();
        sesion.setEstado(SesionClaseEstado.CANCELADA);
        assertThat(violaciones(sesion)).isEmpty();
    }

    @Test
    void estadosExactosDocumentados() {
        assertThat(SesionClaseEstado.values())
                .containsExactly(SesionClaseEstado.PROGRAMADA,
                        SesionClaseEstado.ABIERTA,
                        SesionClaseEstado.CERRADA,
                        SesionClaseEstado.CANCELADA);
    }

    @Test
    void horarioObligatorio() {
        SesionClase sesion = sesionValida();
        sesion.setHorario(null);
        assertThat(violaciones(sesion)).isNotEmpty();
    }

    @Test
    void fechaObligatoria() {
        SesionClase sesion = sesionValida();
        sesion.setFecha(null);
        assertThat(violaciones(sesion)).isNotEmpty();
    }

    @Test
    void estadoObligatorio() {
        SesionClase sesion = sesionValida();
        sesion.setEstado(null);
        assertThat(violaciones(sesion)).isNotEmpty();
    }

    @Test
    void horaAperturaNulable() {
        SesionClase sesion = sesionValida();
        sesion.setHoraApertura(null);
        assertThat(violaciones(sesion)).isEmpty();
    }

    @Test
    void horaAperturaConValorValida() {
        SesionClase sesion = sesionValida();
        sesion.setHoraApertura(OffsetDateTime.now());
        assertThat(violaciones(sesion)).isEmpty();
    }

    @Test
    void horaCierreNulable() {
        SesionClase sesion = sesionValida();
        sesion.setHoraCierre(null);
        assertThat(violaciones(sesion)).isEmpty();
    }

    @Test
    void horaCierreConValorValida() {
        SesionClase sesion = sesionValida();
        sesion.setHoraCierre(OffsetDateTime.now());
        assertThat(violaciones(sesion)).isEmpty();
    }

    @Test
    void fechaEsLocalDate() {
        assertThat(sesionValida().getFecha()).isInstanceOf(LocalDate.class);
    }

    @Test
    void toStringNoIncluyeHorario() {
        SesionClase sesion = sesionValida();
        sesion.setHorario(new Horario());
        assertThat(sesion.toString())
                .contains("fecha=")
                .doesNotContain("Horario{");
    }
}