package edu.dosw.styleradar.estadisticas.domain.model;

public enum MetricCode {
    FOUNDATION_GARMENTS_RECEIVED(ScopeType.FOUNDATION, TimeGranularity.MONTH),
    PLATFORM_GARMENT_PUBLICATIONS(ScopeType.PLATFORM, TimeGranularity.DAY),
    PLATFORM_PLAYLISTS_CREATED(ScopeType.PLATFORM, TimeGranularity.DAY),
    PLATFORM_USER_REGISTRATIONS(ScopeType.PLATFORM, TimeGranularity.DAY),
    PLATFORM_SUBSCRIPTION_REVENUE(ScopeType.PLATFORM, TimeGranularity.MONTH),
    STORE_INVENTORY_BY_STATE(ScopeType.STORE, TimeGranularity.DAY),
    PLATFORM_BUYERS_BY_PLAN(ScopeType.PLATFORM, TimeGranularity.MONTH),
    PLATFORM_STORES_BY_PLAN(ScopeType.PLATFORM, TimeGranularity.MONTH),
    PLATFORM_MONTHLY_DONATIONS(ScopeType.PLATFORM, TimeGranularity.MONTH);

    private final ScopeType scopeType;
    private final TimeGranularity granularity;

    MetricCode(ScopeType scopeType, TimeGranularity granularity) {
        this.scopeType = scopeType;
        this.granularity = granularity;
    }

    public ScopeType scopeType() {
        return scopeType;
    }

    public TimeGranularity granularity() {
        return granularity;
    }
}
