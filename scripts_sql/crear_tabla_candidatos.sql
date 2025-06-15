-- Script para crear la tabla de candidatos en la base de datos de votos
-- Base de datos: votos_elecciones_grajj
-- Host: 10.147.10.101

-- Conectarse a la base de datos:
-- psql -h 10.147.10.101 -U votaciones_grajj -d votos_elecciones_grajj

-- Crear tabla de candidatos
CREATE TABLE IF NOT EXISTS candidato (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    partido VARCHAR(255) NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crear índices para mejorar el rendimiento
CREATE INDEX IF NOT EXISTS idx_candidato_nombre ON candidato(nombre);
CREATE INDEX IF NOT EXISTS idx_candidato_partido ON candidato(partido);

-- Crear trigger para actualizar fecha_actualizacion automáticamente
CREATE OR REPLACE FUNCTION actualizar_fecha_modificacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_actualizar_candidato
    BEFORE UPDATE ON candidato
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_modificacion();

-- Comentarios de documentación
COMMENT ON TABLE candidato IS 'Tabla que almacena información de candidatos para las elecciones';
COMMENT ON COLUMN candidato.id IS 'Identificador único del candidato';
COMMENT ON COLUMN candidato.nombre IS 'Nombre completo del candidato';
COMMENT ON COLUMN candidato.partido IS 'Partido político al que pertenece el candidato';
COMMENT ON COLUMN candidato.fecha_creacion IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN candidato.fecha_actualizacion IS 'Fecha y hora de última actualización del registro';

-- Insertar algunos candidatos de ejemplo (opcional)
-- Descomenta las siguientes líneas si quieres datos de prueba
/*
INSERT INTO candidato (id, nombre, partido) VALUES
(1, 'Juan Pérez', 'Partido Liberal'),
(2, 'María García', 'Partido Conservador'),
(3, 'Carlos López', 'Partido Verde'),
(4, 'Ana Rodríguez', 'Partido Democrático'),
(5, 'Pedro Martínez', 'Partido Socialista')
ON CONFLICT (id) DO NOTHING;
*/

-- Verificar que la tabla se creó correctamente
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default
FROM information_schema.columns 
WHERE table_name = 'candidato' 
ORDER BY ordinal_position;

-- Mostrar cantidad de registros
SELECT COUNT(*) as total_candidatos FROM candidato; 