package edu.dosw.styleradar.estadisticas.domain.model;

public enum MetricCode {
    STORE_PAGE_VISITS(ScopeType.STORE, TimeGranularity.DAY),
    STORE_WISHLIST_SAVES(ScopeType.STORE, TimeGranularity.DAY),
    STORE_GARMENTS_BY_SAVES(ScopeType.STORE, TimeGranularity.DAY),
    STORE_MESSAGES_RECEIVED(ScopeType.STORE, TimeGranularity.DAY),
    FOUNDATION_REQUESTS_FROM_USERS(ScopeType.FOUNDATION, TimeGranularity.MONTH),
    FOUNDATION_REQUESTS_FROM_STORES(ScopeType.FOUNDATION, TimeGranularity.MONTH),
    FOUNDATION_GARMENTS_RECEIVED(ScopeType.FOUNDATION, TimeGranularity.MONTH),
    FOUNDATION_DONATIONS_BY_ORIGIN(ScopeType.FOUNDATION, TimeGranularity.MONTH),
    PLATFORM_SEARCHES(ScopeType.PLATFORM, TimeGranularity.DAY),
    PLATFORM_USER_REGISTRATIONS(ScopeType.PLATFORM, TimeGranularity.DAY),
    PLATFORM_GARMENT_PUBLICATIONS(ScopeType.PLATFORM, TimeGranularity.DAY),
    PLATFORM_PLAYLISTS_CREATED(ScopeType.PLATFORM, TimeGranularity.DAY),
    PLATFORM_MONTHLY_DONATIONS(ScopeType.PLATFORM, TimeGranularity.MONTH),
    MODERATION_PUBLICATIONS(ScopeType.PLATFORM, TimeGranularity.DAY),
    PLATFORM_SUBSCRIPTION_REVENUE(ScopeType.PLATFORM, TimeGranularity.MONTH),
    STORE_INVENTORY_BY_STATE(ScopeType.STORE, TimeGranularity.DAY),
    FOUNDATION_GARMENTS_BY_TYPE(ScopeType.FOUNDATION, TimeGranularity.MONTH),
    FOUNDATION_REQUESTS_BY_STATE(ScopeType.FOUNDATION, TimeGranularity.MONTH),
    PLATFORM_BUYERS_BY_PLAN(ScopeType.PLATFORM, TimeGranularity.MONTH),
    PLATFORM_STORES_BY_PLAN(ScopeType.PLATFORM, TimeGranularity.MONTH),
    PLATFORM_STORES_BY_VERIFICATION(ScopeType.PLATFORM, TimeGranularity.DAY),
    STORE_REALTIME_VISITORS(ScopeType.STORE, TimeGranularity.DAY, true),
    PLATFORM_REALTIME_USERS(ScopeType.PLATFORM, TimeGranularity.DAY, true),
    PLATFORM_ACTIVE_USERS(ScopeType.PLATFORM, TimeGranularity.DAY, true),
    PLATFORM_ACTIVE_FOUNDATIONS(ScopeType.PLATFORM, TimeGranularity.DAY, true),
    PLATFORM_ALERTS_CREATED(ScopeType.PLATFORM, TimeGranularity.DAY);

    private final ScopeType scopeType;
    private final TimeGranularity granularity;
    private final boolean realTime;

    MetricCode(ScopeType scopeType, TimeGranularity granularity) {
        this(scopeType, granularity, false);
    }

    MetricCode(ScopeType scopeType, TimeGranularity granularity, boolean realTime) {
        this.scopeType = scopeType;
        this.granularity = granularity;
        this.realTime = realTime;
    }

    public ScopeType scopeType() {
        return scopeType;
    }

    public TimeGranularity granularity() {
        return granularity;
    }

    public boolean isRealTime() {
        return realTime;
    }
}
