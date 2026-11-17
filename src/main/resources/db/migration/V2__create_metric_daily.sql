CREATE TABLE IF NOT EXISTS metric_daily (
    id UUID PRIMARY KEY,
    scope_type VARCHAR(30) NOT NULL,
    scope_id VARCHAR(100) NOT NULL,
    metric_code VARCHAR(100) NOT NULL,
    metric_date DATE NOT NULL,
    value NUMERIC(18, 2) NOT NULL,
    CONSTRAINT uq_metric_daily_scope_metric_date
        UNIQUE (scope_type, scope_id, metric_code, metric_date)
);
