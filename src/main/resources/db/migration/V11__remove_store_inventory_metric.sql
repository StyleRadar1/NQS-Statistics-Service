DELETE FROM metric_distribution
WHERE metric_code = 'STORE_INVENTORY_BY_STATE'
   OR scope_type = 'STORE';

DELETE FROM metric_daily
WHERE metric_code = 'STORE_INVENTORY_BY_STATE'
   OR scope_type = 'STORE';
