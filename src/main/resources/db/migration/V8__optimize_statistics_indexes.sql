CREATE INDEX IF NOT EXISTS idx_metric_daily_lookup
    ON metric_daily (scope_type, scope_id, metric_code, metric_date);

CREATE INDEX IF NOT EXISTS idx_metric_goal_lookup
    ON metric_goal (scope_type, scope_id, metric_code, period_start_date);
