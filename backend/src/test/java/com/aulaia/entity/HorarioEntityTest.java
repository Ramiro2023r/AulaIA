package com.aulaia.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de las validaciones del modelo {@link Horario}
 * (Prompt 5.2, 07-PLAN: dia 1–7, horaFin &gt; horaInicio, tolerancia &gt;= 0;
 * 04-BD §6.7: minutos_antes_apertura &gt;= 0, relaciones obligatorias).
 * Usa el validador Bean Validation real. Datos ficticios.
 */
class HorarioEntityTest {

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

    private Horario horarioValido() {
        Horario horario = new Horario();
        horario.setCurso(new Curso());
        horario.setSeccion(new Seccion());
        horario.setDocente(new Docente());
        horario.setDiaSemana((short) 1);
        horario.setHoraInicio(LocalTime.of(9, 0));
        horario.setHoraFin(LocalTime.of(10, 30));
        horario.setToleranciaMinutos((short) 10);
        horario.setMinutosAntesApertura((short) 15);
        return horario;
    }

    private Set<?> violaciones(Horario horario) {
        return validator.validate(horario);
    }

    @Test
    void horarioCompletoValido() {
        assertThat(violaciones(horarioValido())).isEmpty();
    }

    @Test
    void diaSemanaUnoValido() {
        Horario horario = horarioValido();
        horario.setDiaSemana((short) 1);
        assertThat(violaciones(horario)).isEmpty();
    }

    @Test
    void diaSemanaSieteValido() {
        Horario horario = horarioValido();
        horario.setDiaSemana((short) 7);
        assertThat(violaciones(horario)).isEmpty();
    }

    @Test
    void diaSemanaCeroInvalido() {
        Horario horario = horarioValido();
        horario.setDiaSemana((short) 0);
        assertThat(violaciones(horario)).isNotEmpty();
    }

    @Test
    void diaSemanaOchoInvalido() {
        Horario horario = horarioValido();
        horario.setDiaSemana((short) 8);
        assertThat(violaciones(horario)).isNotEmpty();
    }

    @Test
    void horaFinPosteriorValido() {
        Horario horario = horarioValido();
        horario.setHoraFin(LocalTime.of(23, 59));
        assertThat(violaciones(horario)).isEmpty();
    }

    @Test
    void horaFinIgualInvalido() {
        Horario horario = horarioValido();
        horario.setHoraFin(LocalTime.of(9, 0));
        assertThat(violaciones(horario)).isNotEmpty();
    }

    @Test
    void horaFinAnteriorInvalido() {
        Horario horario = horarioValido();
        horario.setHoraFin(LocalTime.of(8, 0));
        assertThat(violaciones(horario)).isNotEmpty();
    }

    @Test
    void horarioQueCruzaMedianocheInvalido() {
        Horario horario = horarioValido();
        horario.setHoraInicio(LocalTime.of(22, 0));
        horario.setHoraFin(LocalTime.of(2, 0));
        assertThat(violaciones(horario)).isNotEmpty();
    }

    @Test
    void toleranciaCeroValida() {
        Horario horario = horarioValido();
        horario.setToleranciaMinutos((short) 0);
        assertThat(violaciones(horario)).isEmpty();
    }

    @Test
    void toleranciaPositivaValida() {
        Horario horario = horarioValido();
        horario.setToleranciaMinutos((short) 30);
        assertThat(violaciones(horario)).isEmpty();
    }

    @Test
    void toleranciaNegativaInvalida() {
        Horario horario = horarioValido();
        horario.setToleranciaMinutos((short) -1);
        assertThat(violaciones(horario)).isNotEmpty();
    }

    @Test
    void minutosAntesAperturaCeroValido() {
        Horario horario = horarioValido();
        horario.setMinutosAntesApertura((short) 0);
        assertThat(violaciones(horario)).isEmpty();
    }

    @Test
    void minutosAntesAperturaNegativoInvalido() {
        Horario horario = horarioValido();
        horario.setMinutosAntesApertura((short) -1);
        assertThat(violaciones(horario)).isNotEmpty();
    }

    @Test
    void relacionesObligatorias() {
        Horario sinCurso = horarioValido();
        sinCurso.setCurso(null);
        assertThat(violaciones(sinCurso)).isNotEmpty();

        Horario sinSeccion = horarioValido();
        sinSeccion.setSeccion(null);
        assertThat(violaciones(sinSeccion)).isNotEmpty();

        Horario sinDocente = horarioValido();
        sinDocente.setDocente(null);
        assertThat(violaciones(sinDocente)).isNotEmpty();
    }

    @Test
    void horasObligatorias() {
        Horario sinInicio = horarioValido();
        sinInicio.setHoraInicio(null);
        assertThat(violaciones(sinInicio)).isNotEmpty();

        Horario sinFin = horarioValido();
        sinFin.setHoraFin(null);
        assertThat(violaciones(sinFin)).isNotEmpty();
    }
}