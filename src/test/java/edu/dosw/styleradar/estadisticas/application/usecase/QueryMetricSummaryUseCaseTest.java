package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryMetricSummaryUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC);
    private static final Scope STORE_SCOPE = new Scope("store-17", ScopeType.STORE);

    private StubMetricRepository repository;
    private QueryMetricSummaryUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repository = new StubMetricRepository();
        useCase = new QueryMetricSummaryUseCaseImpl(repository, CLOCK);
    }

    @Test
    void queriesAllTimeAndCurrentMonthTotalsForFoundationAndPlatformMetrics() {
        Scope foundationScope = new Scope("foundation-7", ScopeType.FOUNDATION);
        Scope platformScope = new Scope("platform", ScopeType.PLATFORM);

        repository.allTimeTotals.put(MetricCode.FOUNDATION_GARMENTS_RECEIVED, BigDecimal.valueOf(145));
        repository.allTimeTotals.put(MetricCode.PLATFORM_GARMENT_PUBLICATIONS, BigDecimal.valueOf(980));
        repository.allTimeTotals.put(MetricCode.PLATFORM_PLAYLISTS_CREATED, null);
        Period currentMonth = new Period(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"));
        repository.put(currentMonth, List.of(
                new Point(LocalDate.parse("2026-11-03"), BigDecimal.valueOf(12)),
                new Point(LocalDate.parse("2026-11-20"), BigDecimal.valueOf(18))));

        var received = useCase.queryTotal(MetricCode.FOUNDATION_GARMENTS_RECEIVED, foundationScope);
        assertEquals(0, BigDecimal.valueOf(145).compareTo(received.total()));
        assertNull(received.period());

        var garments = useCase.queryTotal(MetricCode.PLATFORM_GARMENT_PUBLICATIONS, platformScope);
        assertEquals(0, BigDecimal.valueOf(980).compareTo(garments.total()));
        assertNull(garments.period());

        var playlists = useCase.queryTotal(MetricCode.PLATFORM_PLAYLISTS_CREATED, platformScope);
        assertEquals(0, BigDecimal.ZERO.compareTo(playlists.total()));
        assertNull(playlists.period());

        var monthlyDonations = useCase.queryTotal(MetricCode.PLATFORM_MONTHLY_DONATIONS, platformScope);
        assertEquals(0, BigDecimal.valueOf(30).compareTo(monthlyDonations.total()));
        assertEquals(currentMonth, monthlyDonations.period());

        assertThrows(UnauthorizedScopeException.class, () -> useCase.queryTotal(
                MetricCode.FOUNDATION_GARMENTS_RECEIVED, platformScope));
        assertThrows(UnsupportedMetricScopeException.class, () -> useCase.queryTotal(
                MetricCode.STORE_INVENTORY_BY_STATE, STORE_SCOPE));
    }

    private static final class StubMetricRepository implements MetricRepositoryPort {
        private final Map<Period, List<Point>> pointsByPeriod = new HashMap<>();
        private final Map<MetricCode, BigDecimal> allTimeTotals = new HashMap<>();

        void put(Period period, List<Point> points) {
            pointsByPeriod.put(period, points);
        }

        @Override
        public List<Point> findDailyPoints(MetricCode metricCode, Scope scope, Period period) {
            return pointsByPeriod.getOrDefault(period, List.of());
        }

        @Override
        public BigDecimal sumAllDailyPoints(MetricCode metricCode, Scope scope) {
            return allTimeTotals.get(metricCode);
        }

        @Override
        public List<DistributionCount> findDistributionCounts(MetricCode metricCode, Scope scope) {
            return List.of();
        }
    }
}
