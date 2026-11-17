CREATE TABLE IF NOT EXISTS metric_goal (
    id UUID PRIMARY KEY,
    scope_type VARCHAR(30) NOT NULL,
    scope_id VARCHAR(100) NOT NULL,
    metric_code VARCHAR(100) NOT NULL,
    period_start_date DATE NOT NULL,
    target_value NUMERIC(18, 2) NOT NULL CHECK (target_value > 0),
    CONSTRAINT uq_metric_goal_scope_metric_period
        UNIQUE (scope_type, scope_id, metric_code, period_start_date)
);
