CREATE TABLE telegram_vinculaciones (
    id SERIAL PRIMARY KEY,
    estudiante_id INTEGER NOT NULL,
    apoderado_id INTEGER,
    token VARCHAR(64) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_tv_estudiante FOREIGN KEY (estudiante_id) REFERENCES estudiantes (id) ON DELETE CASCADE,
    CONSTRAINT fk_tv_apoderado FOREIGN KEY (apoderado_id) REFERENCES apoderados (id) ON DELETE SET NULL
);

CREATE INDEX idx_tv_token ON telegram_vinculaciones(token);
CREATE INDEX idx_tv_estudiante ON telegram_vinculaciones(estudiante_id);
