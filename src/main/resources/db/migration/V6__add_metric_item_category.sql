ALTER TABLE metric_item_daily
    ADD COLUMN IF NOT EXISTS category_code VARCHAR(100) NOT NULL DEFAULT 'UNSPECIFIED';

ALTER TABLE metric_item_daily
    DROP CONSTRAINT IF EXISTS uq_metric_item_daily_scope_metric_item_day;

ALTER TABLE metric_item_daily
    ADD CONSTRAINT uq_metric_item_daily_scope_metric_item_day
        UNIQUE (scope_type, scope_id, metric_code, category_code, item_id, day);
