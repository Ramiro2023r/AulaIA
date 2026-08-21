-- ============================================================
-- AulaIA — Migración V9: tabla sesiones_clase (docs/04-BASE_DE_DATOS §7.1)
-- ============================================================
-- Una sesión de clase representa una ocurrencia real de un horario
-- (plantilla semanal) en una fecha específica.
--
-- Modelo oficial (04-BD §7.1): id, horario_id (FK), fecha DATE,
-- hora_apertura TIMESTAMPTZ NULL, hora_cierre TIMESTAMPTZ NULL,
-- estado VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADA', created_at,
-- updated_at.
--
-- Estados permitidos (04-BD §7.1): PROGRAMADA, ABIERTA, CERRADA,
-- CANCELADA. El enum Java restringe el lado de aplicación; 04-BD NO
-- define CHECK físico para estado, por lo que NO se agrega por
-- iniciativa propia.
--
-- Restricciones físicas documentadas (04-BD §7.1):
--   - FK fk_sesiones_horario → horarios(id).
--   - UNIQUE uq_sesion_horario_fecha (horario_id, fecha): un horario
--     solo puede tener una sesión por fecha.
--
-- Índices recomendados documentados (04-BD §14): idx_sesiones_fecha,
-- idx_sesiones_estado.
--
-- Sin ON DELETE CASCADE: los documentos no lo exigen.
-- Sin triggers, sin seeds: solo estructura.
-- ============================================================

CREATE TABLE sesiones_clase (
    id BIGSERIAL PRIMARY KEY,
    horario_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    hora_apertura TIMESTAMPTZ NULL,
    hora_cierre TIMESTAMPTZ NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADA',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sesiones_horario
        FOREIGN KEY (horario_id)
        REFERENCES horarios(id),

    CONSTRAINT uq_sesion_horario_fecha
        UNIQUE (horario_id, fecha)
);

CREATE INDEX idx_sesiones_fecha
    ON sesiones_clase(fecha);

CREATE INDEX idx_sesiones_estado
    ON sesiones_clase(estado);