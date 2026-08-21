-- V10__create_asistencias.sql
-- Sprint 7.1 — Entidad Asistencia
-- Fuente de verdad: docs/04-BASE_DE_DATOS_AulaIA.md §8.1, §14, §15
-- Restricciones: fk_asistencias_sesion, fk_asistencias_estudiante,
--                uq_asistencia_sesion_estudiante, ck_asistencia_estado,
--                ck_asistencia_metodo

CREATE TABLE asistencias
(
    id              BIGSERIAL PRIMARY KEY,
    sesion_clase_id BIGINT                   NOT NULL,
    estudiante_id   BIGINT                   NOT NULL,
    fecha_hora      TIMESTAMPTZ              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado          VARCHAR(30)              NOT NULL,
    metodo          VARCHAR(30)              NOT NULL,
    observacion     VARCHAR(500)             NULL,
    created_at      TIMESTAMPTZ              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ              NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_asistencias_sesion
        FOREIGN KEY (sesion_clase_id)
            REFERENCES sesiones_clase (id),

    CONSTRAINT fk_asistencias_estudiante
        FOREIGN KEY (estudiante_id)
            REFERENCES estudiantes (id),

    CONSTRAINT uq_asistencia_sesion_estudiante
        UNIQUE (sesion_clase_id, estudiante_id),

    CONSTRAINT ck_asistencia_estado
        CHECK (estado IN (
            'PRESENTE',
            'TARDANZA',
            'AUSENTE',
            'JUSTIFICADO'
        )),

    CONSTRAINT ck_asistencia_metodo
        CHECK (metodo IN (
            'QR',
            'CODIGO',
            'MANUAL_DOCENTE',
            'SISTEMA'
        ))
);

-- Índices documentados en 04-BD §14 para tabla asistencias
CREATE INDEX idx_asistencias_estudiante
    ON asistencias (estudiante_id);

CREATE INDEX idx_asistencias_estado
    ON asistencias (estado);

CREATE INDEX idx_asistencias_fecha_hora
    ON asistencias (fecha_hora);

-- Nota: idx_asistencias_sesion_estado (compuesto) es descrito como
-- "recomendado" en §14 al igual que todos los demás, pero se omite
-- en este prompt por no haber determinación inequívoca de obligatoriedad
-- diferenciada. Se añadirá en prompt posterior si se requiere para
-- rendimiento en consultas de sesión.
