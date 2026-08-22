-- V15 fue aplicada con SERIAL (INTEGER), pero la entidad usa Long.
-- Se amplía sin recrear la tabla ni perder los valores existentes.
ALTER SEQUENCE telegram_vinculaciones_id_seq AS BIGINT;

ALTER TABLE telegram_vinculaciones
    ALTER COLUMN id TYPE BIGINT;

ALTER TABLE telegram_vinculaciones
    ALTER COLUMN id SET DEFAULT nextval('telegram_vinculaciones_id_seq'::regclass);
