package com.aulaia.dto.dashboard;

import java.time.OffsetDateTime;

public record AsistenciaRecienteDto(
        String estudianteNombre,
        String cursoNombre,
        String estado,
        OffsetDateTime horaRegistro
) {}
