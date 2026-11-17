package edu.dosw.styleradar.estadisticas.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainValidationTest {

    @Test
    void validatesAnalyticsEventAndDailyMetricIncrement() {
        Scope storeScope = new Scope("store-1", ScopeType.STORE);
        Instant now = Instant.parse("2026-11-17T10:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> new AnalyticsEvent(
                "", MetricCode.STORE_PAGE_VISITS, storeScope, BigDecimal.ONE, now, null));
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsEvent(
                "evt-1", MetricCode.STORE_PAGE_VISITS, storeScope, BigDecimal.ZERO, now, null));
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsEvent(
                "evt-1", MetricCode.PLATFORM_SEARCHES, storeScope, BigDecimal.ONE, now, null));

        assertThrows(IllegalArgumentException.class, () -> new DailyMetricIncrement(
                MetricCode.PLATFORM_SEARCHES, storeScope, LocalDate.of(2026, 11, 17), BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new DailyMetricIncrement(
                MetricCode.STORE_PAGE_VISITS, storeScope, LocalDate.of(2026, 11, 17), BigDecimal.ZERO));
    }

    @Test
    void validatesCategoryDistributionAndGoalProgressRecords() {
        Scope storeScope = new Scope("store-1", ScopeType.STORE);
        Period period = new Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7));

        assertThrows(IllegalArgumentException.class, () -> new CategoryCode("   "));
        assertThrows(IllegalArgumentException.class, () -> new DistributionCount(CategoryCode.AVAILABLE, -1));
        assertThrows(IllegalArgumentException.class, () -> new DistributionEntry(
                CategoryCode.AVAILABLE, -1, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new DistributionEntry(
                CategoryCode.AVAILABLE, 1, new BigDecimal("-0.1")));
        assertThrows(IllegalArgumentException.class, () -> new DistributionEntry(
                CategoryCode.AVAILABLE, 1, new BigDecimal("100.1")));

        DistributionEntry duplicate = new DistributionEntry(CategoryCode.AVAILABLE, 1, new BigDecimal("100.0"));
        assertThrows(IllegalArgumentException.class, () -> new Distribution(
                MetricCode.STORE_INVENTORY_BY_STATE, storeScope, List.of(duplicate, duplicate)));

        assertThrows(IllegalArgumentException.class, () -> new GoalProgress(
                MetricCode.STORE_PAGE_VISITS,
                storeScope,
                period,
                BigDecimal.ONE,
                BigDecimal.TEN,
                null,
                GoalProgressStatus.GOAL_NOT_DEFINED));
        assertThrows(IllegalArgumentException.class, () -> new GoalProgress(
                MetricCode.STORE_PAGE_VISITS,
                storeScope,
                period,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                GoalProgressStatus.GOAL_DEFINED));
    }

    @Test
    void validatesModerationRankingRealTimePeriodAndScopeRecords() {
        Scope storeScope = new Scope("store-1", ScopeType.STORE);

        assertThrows(IllegalArgumentException.class, () -> new Scope(" ", ScopeType.STORE));
        assertThrows(IllegalArgumentException.class, () -> new Period(
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9)));
        assertThrows(IllegalArgumentException.class, () -> new RealTimeCount(
                MetricCode.STORE_REALTIME_VISITORS, storeScope, -1));

        assertThrows(IllegalArgumentException.class, () -> new GarmentSavesCount("", "Prenda", 1));
        assertThrows(IllegalArgumentException.class, () -> new GarmentSavesCount("g1", " ", 1));
        assertThrows(IllegalArgumentException.class, () -> new GarmentSavesCount("g1", "Prenda", -1));

        assertThrows(IllegalArgumentException.class, () -> new RankedItem("", "Prenda", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RankedItem("g1", "", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RankedItem("g1", "Prenda", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new RankedItem("g1", "Prenda", 1, -1));

        RankedItem item = new RankedItem("g1", "Prenda", 1, 5);
        assertEquals(1, item.position());
        assertThrows(IllegalArgumentException.class, () -> new Ranking(
                MetricCode.STORE_GARMENTS_BY_SAVES, storeScope, 0, List.of(item), null));
        assertThrows(IllegalArgumentException.class, () -> new Ranking(
                MetricCode.STORE_GARMENTS_BY_SAVES, storeScope, 1, List.of(item, item), null));
        assertThrows(IllegalArgumentException.class, () -> new Ranking(
                MetricCode.STORE_GARMENTS_BY_SAVES, storeScope, 6, List.of(), " "));

        assertThrows(IllegalArgumentException.class, () -> new PendingModerationPublisher("", "Store", 1));
        assertThrows(IllegalArgumentException.class, () -> new PendingModerationPublisher("s1", "", 1));
        assertThrows(IllegalArgumentException.class, () -> new PendingModerationPublisher("s1", "Store", 0));

        assertThrows(IllegalArgumentException.class, () -> new PendingModerationPublisherCount(
                CategoryCode.ORIGIN_STORE, "", "Store", 1));
        assertThrows(IllegalArgumentException.class, () -> new PendingModerationPublisherCount(
                CategoryCode.ORIGIN_STORE, "s1", "", 1));
        assertThrows(IllegalArgumentException.class, () -> new PendingModerationPublisherCount(
                CategoryCode.ORIGIN_STORE, "s1", "Store", 0));

        assertThrows(IllegalArgumentException.class, () -> new PendingModeration(List.of(), -1, "msg"));
        assertThrows(IllegalArgumentException.class, () -> new PendingModeration(List.of(), 0, " "));
    }
}
