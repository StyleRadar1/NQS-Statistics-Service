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
    void validatesDistributionRecords() {
        Scope storeScope = new Scope("store-1", ScopeType.STORE);

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
    }

    @Test
    void validatesScopePeriodMetricTotalAndDomainAnalyticsEvent() {
        assertThrows(IllegalArgumentException.class, () -> new Scope(" ", ScopeType.STORE));
        assertThrows(IllegalArgumentException.class, () -> new Period(
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9)));

        assertEquals(BigDecimal.TEN, new MetricTotal(
                MetricCode.FOUNDATION_GARMENTS_RECEIVED,
                new Scope("foundation-1", ScopeType.FOUNDATION),
                null,
                BigDecimal.TEN).total());
        assertThrows(IllegalArgumentException.class, () -> new MetricTotal(
                MetricCode.FOUNDATION_GARMENTS_RECEIVED,
                new Scope("foundation-1", ScopeType.FOUNDATION),
                null,
                BigDecimal.valueOf(-1)));

        assertThrows(IllegalArgumentException.class, () -> new DomainAnalyticsEvent(
                " ", DomainEventType.PLAYLIST_CREATED, Instant.now(),
                null, null, null, BigDecimal.ONE, 0L, null));
        assertThrows(IllegalArgumentException.class, () -> new DomainAnalyticsEvent(
                "evt-neg", DomainEventType.PLAYLIST_CREATED, Instant.now(),
                null, null, null, BigDecimal.valueOf(-1), 0L, null));
    }
}
