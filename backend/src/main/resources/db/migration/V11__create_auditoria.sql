-- 04-BD §11.1
CREATE TABLE auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NULL,
    entidad VARCHAR(80) NOT NULL,
    entidad_id BIGINT NULL,
    accion VARCHAR(80) NOT NULL,
    valor_anterior JSONB NULL,
    valor_nuevo JSONB NULL,
    ip_origen VARCHAR(64) NULL,
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_auditoria_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
);

CREATE INDEX idx_auditoria_entidad ON auditoria(entidad);
CREATE INDEX idx_auditoria_fecha ON auditoria(fecha_hora);
