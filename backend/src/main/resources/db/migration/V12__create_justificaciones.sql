-- Migración para crear la tabla de justificaciones (Prompt 14.3)
-- docs/04-BASE_DE_DATOS_AulaIA.md §10

CREATE TABLE justificaciones (
    id BIGSERIAL PRIMARY KEY,
    asistencia_id BIGINT NOT NULL UNIQUE,
    motivo VARCHAR(500) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    revisado_por_usuario_id BIGINT NULL,
    fecha_revision TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_justificaciones_asistencia
        FOREIGN KEY (asistencia_id)
        REFERENCES asistencias(id),

    CONSTRAINT fk_justificaciones_usuario
        FOREIGN KEY (revisado_por_usuario_id)
        REFERENCES usuarios(id),

    CONSTRAINT ck_justificaciones_estado
        CHECK (estado IN (
            'PENDIENTE',
            'APROBADA',
            'RECHAZADA'
        ))
);

-- Índices para mejorar las consultas
CREATE INDEX idx_justificaciones_estado ON justificaciones(estado);
