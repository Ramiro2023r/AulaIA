-- V13__add_docente_profile_fields.sql
-- Añadir campos de perfil a la tabla de docentes

ALTER TABLE docentes
ADD COLUMN correo_alternativo VARCHAR(100),
ADD COLUMN telefono VARCHAR(20),
ADD COLUMN biografia TEXT;
