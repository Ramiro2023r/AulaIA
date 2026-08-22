-- V15 creó ambas columnas FK como INTEGER aunque sus entidades usan IDs Long.
-- Se preservan los datos y las reglas ON DELETE originales al recrear las FK.
ALTER TABLE telegram_vinculaciones
    DROP CONSTRAINT fk_tv_estudiante,
    DROP CONSTRAINT fk_tv_apoderado;

ALTER TABLE telegram_vinculaciones
    ALTER COLUMN estudiante_id TYPE BIGINT USING estudiante_id::BIGINT,
    ALTER COLUMN apoderado_id TYPE BIGINT USING apoderado_id::BIGINT;

ALTER TABLE telegram_vinculaciones
    ADD CONSTRAINT fk_tv_estudiante
        FOREIGN KEY (estudiante_id) REFERENCES estudiantes (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_tv_apoderado
        FOREIGN KEY (apoderado_id) REFERENCES apoderados (id) ON DELETE SET NULL;
