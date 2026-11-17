CREATE TABLE IF NOT EXISTS metric_distribution (
    id UUID PRIMARY KEY,
    scope_type VARCHAR(30) NOT NULL,
    scope_id VARCHAR(100) NOT NULL,
    metric_code VARCHAR(100) NOT NULL,
    category_code VARCHAR(100) NOT NULL,
    value BIGINT NOT NULL CHECK (value >= 0),
    CONSTRAINT uq_metric_distribution_scope_metric_category
        UNIQUE (scope_type, scope_id, metric_code, category_code)
);

CREATE INDEX IF NOT EXISTS idx_metric_distribution_scope_metric
    ON metric_distribution (scope_type, scope_id, metric_code);
