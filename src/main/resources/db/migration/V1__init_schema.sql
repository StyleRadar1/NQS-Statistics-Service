-- ========================================================================
-- Schema V1: Modelos de Estadisticas (Tableros, Graficas, Series, Puntos, Indicadores)
-- ========================================================================

CREATE TABLE IF NOT EXISTS tableros (
    id UUID PRIMARY KEY,
    id_dueno UUID NOT NULL,
    tipo VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS graficas (
    id UUID PRIMARY KEY,
    id_tablero UUID NOT NULL,
    titulo VARCHAR(100) NOT NULL,
    CONSTRAINT fk_grafica_tablero FOREIGN KEY (id_tablero) REFERENCES tableros(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS series (
    id UUID PRIMARY KEY,
    id_grafica UUID NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    CONSTRAINT fk_serie_grafica FOREIGN KEY (id_grafica) REFERENCES graficas(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS puntos (
    id UUID PRIMARY KEY,
    id_serie UUID NOT NULL,
    fecha DATE NOT NULL,
    valor NUMERIC(15, 2) NOT NULL,
    CONSTRAINT fk_punto_serie FOREIGN KEY (id_serie) REFERENCES series(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS indicadores (
    id UUID PRIMARY KEY,
    id_tablero UUID NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    valor NUMERIC(15, 2) NOT NULL DEFAULT 0,
    unidad VARCHAR(100) NOT NULL,
    fecha TIMESTAMP NOT NULL,
    periodo_inicio DATE,
    periodo_fin DATE,
    valor_anterior NUMERIC(15, 2) DEFAULT 0,
    variacion_porcentual NUMERIC(10, 2) DEFAULT 0,
    CONSTRAINT fk_indicador_tablero FOREIGN KEY (id_tablero) REFERENCES tableros(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tableros_tipo_dueno ON tableros(tipo, id_dueno);
CREATE INDEX IF NOT EXISTS idx_indicadores_tablero_periodo ON indicadores(id_tablero, periodo_inicio, periodo_fin);
CREATE INDEX IF NOT EXISTS idx_graficas_tablero ON graficas(id_tablero);
CREATE INDEX IF NOT EXISTS idx_series_grafica ON series(id_grafica);
CREATE INDEX IF NOT EXISTS idx_puntos_serie ON puntos(id_serie);
