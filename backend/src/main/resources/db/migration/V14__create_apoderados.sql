-- 1. Crear tabla apoderados
CREATE TABLE apoderados (
    id BIGSERIAL PRIMARY KEY,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(30) NULL,
    telegram_chat_id VARCHAR(50) NULL UNIQUE,
    telegram_vinculado_at TIMESTAMPTZ NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Crear tabla intermedia para normalizar relación Estudiante-Apoderado
CREATE TABLE estudiante_apoderados (
    id BIGSERIAL PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
    apoderado_id BIGINT NOT NULL,
    parentesco VARCHAR(30) NOT NULL,
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_estudiante_apoderados_estudiante
        FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id),
    CONSTRAINT fk_estudiante_apoderados_apoderado
        FOREIGN KEY (apoderado_id) REFERENCES apoderados(id),
    CONSTRAINT uq_estudiante_apoderado
        UNIQUE (estudiante_id, apoderado_id)
);

CREATE INDEX idx_estudiante_apoderados_estudiante ON estudiante_apoderados(estudiante_id);
CREATE INDEX idx_estudiante_apoderados_apoderado ON estudiante_apoderados(apoderado_id);
