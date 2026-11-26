DELETE FROM metric_distribution
WHERE (metric_code = 'STORE_INVENTORY_BY_STATE' AND category_code NOT IN ('AVAILABLE', 'LOW_STOCK', 'OUT_OF_STOCK'))
   OR (metric_code = 'PLATFORM_BUYERS_BY_PLAN' AND category_code NOT IN ('PLAN_RADAR', 'PLAN_RADAR_PLUS'))
   OR (metric_code = 'PLATFORM_STORES_BY_PLAN' AND category_code NOT IN ('PLAN_STORE_MONTHLY', 'SUSPENDED'));
