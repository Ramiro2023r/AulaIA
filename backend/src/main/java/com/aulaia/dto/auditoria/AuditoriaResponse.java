package com.aulaia.dto.auditoria;

import java.time.OffsetDateTime;

public record AuditoriaResponse(
        Long id,
        String usuarioUsername,
        String entidad,
        Long entidadId,
        String accion,
        String valorAnterior,
        String valorNuevo,
        String ipOrigen,
        OffsetDateTime fechaHora
) {}
