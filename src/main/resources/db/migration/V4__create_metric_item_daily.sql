CREATE TABLE IF NOT EXISTS metric_item_daily (
    id UUID PRIMARY KEY,
    scope_type VARCHAR(30) NOT NULL,
    scope_id VARCHAR(100) NOT NULL,
    metric_code VARCHAR(100) NOT NULL,
    item_id VARCHAR(100) NOT NULL,
    item_name VARCHAR(160) NOT NULL,
    day DATE NOT NULL,
    value BIGINT NOT NULL CHECK (value >= 0),
    CONSTRAINT uq_metric_item_daily_scope_metric_item_day
        UNIQUE (scope_type, scope_id, metric_code, item_id, day)
);

CREATE INDEX IF NOT EXISTS idx_metric_item_daily_scope_metric
    ON metric_item_daily (scope_type, scope_id, metric_code);
