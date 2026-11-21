package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.application.service.TimeSeriesGapFiller;
import edu.dosw.styleradar.estadisticas.domain.exception.InvalidTimeSeriesRequestException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.ComparisonWindow;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.GarmentSavesCount;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.model.TimeGranularity;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryTimeSeriesUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);

    private final RecordingMetricRepository repository = new RecordingMetricRepository();
    private QueryTimeSeriesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new QueryTimeSeriesUseCaseImpl(repository, new TimeSeriesGapFiller(), CLOCK);
        repository.points = List.of();
        repository.calls = 0;
    }

    @Test
    void returnsSevenDailyPointsWithZerosForMissingDays() {
        repository.points = List.of(
                new Point(LocalDate.parse("2026-11-02"), BigDecimal.valueOf(2)),
                new Point(LocalDate.parse("2026-11-06"), BigDecimal.valueOf(5)));
        Period period = new Period(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-07"));

        var result = useCase.query(
                MetricCode.STORE_PAGE_VISITS,
                new Scope("store-1", ScopeType.STORE),
                period,
                TimeGranularity.DAY);

        assertEquals(7, result.points().size());
        assertEquals(5, result.points().stream().filter(point -> point.value().signum() == 0).count());
        assertEquals(1, repository.calls);
        assertEquals(period, repository.lastPeriod);
    }

    @Test
    void foundationRequestMetricsAlwaysReturnExactlyTheLatestSixMonths() {
        Period suppliedPeriod = new Period(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"));

        for (MetricCode metricCode : List.of(
                MetricCode.FOUNDATION_REQUESTS_FROM_USERS,
                MetricCode.FOUNDATION_REQUESTS_FROM_STORES)) {
            var result = useCase.query(
                    metricCode,
                    new Scope("foundation-1", ScopeType.FOUNDATION),
                    suppliedPeriod,
                    TimeGranularity.MONTH);

            assertEquals(6, result.points().size(), metricCode.name());
            assertEquals(LocalDate.parse("2026-06-01"), result.points().getFirst().date());
            assertEquals(LocalDate.parse("2026-11-01"), result.points().getLast().date());
            assertEquals(LocalDate.parse("2026-06-01"), result.period().startDate());
            assertEquals(LocalDate.parse("2026-11-30"), result.period().endDate());
        }
    }

    @Test
    void rejectsWrongScopeAndGranularityBeforeAccessingRepository() {
        assertThrows(UnauthorizedScopeException.class, () -> useCase.query(
                MetricCode.STORE_PAGE_VISITS,
                new Scope("platform", ScopeType.PLATFORM),
                null,
                TimeGranularity.DAY));

        assertThrows(InvalidTimeSeriesRequestException.class, () -> useCase.query(
                MetricCode.STORE_PAGE_VISITS,
                new Scope("store-1", ScopeType.STORE),
                null,
                TimeGranularity.MONTH));

        assertThrows(UnsupportedMetricScopeException.class, () -> useCase.query(
                MetricCode.STORE_REALTIME_VISITORS,
                new Scope("store-1", ScopeType.STORE),
                null,
                null));

        assertEquals(0, repository.calls);
    }

    @Test
    void handlesPlatformDailyAndMonthlyRevenueSeriesAndDefaults() {
        var storeDefault = useCase.query(
                MetricCode.STORE_PAGE_VISITS,
                new Scope("store-1", ScopeType.STORE),
                null,
                null);
        assertEquals(LocalDate.parse("2026-11-01"), storeDefault.period().startDate());
        assertEquals(LocalDate.parse("2026-11-17"), storeDefault.period().endDate());

        var platformDailyDefault = useCase.query(
                MetricCode.PLATFORM_SEARCHES,
                new Scope("platform", ScopeType.PLATFORM),
                null,
                null);
        assertEquals(LocalDate.parse("2026-11-01"), platformDailyDefault.period().startDate());
        assertEquals(LocalDate.parse("2026-11-30"), platformDailyDefault.period().endDate());

        assertThrows(InvalidTimeSeriesRequestException.class, () -> useCase.query(
                MetricCode.PLATFORM_SEARCHES,
                new Scope("platform", ScopeType.PLATFORM),
                new Period(LocalDate.parse("2026-10-15"), LocalDate.parse("2026-11-15")),
                TimeGranularity.DAY));

        var revenueDefault = useCase.query(
                MetricCode.PLATFORM_SUBSCRIPTION_REVENUE,
                new Scope("platform", ScopeType.PLATFORM),
                null,
                TimeGranularity.MONTH);
        assertEquals(6, revenueDefault.points().size());
        assertEquals(LocalDate.parse("2026-06-01"), revenueDefault.period().startDate());
        assertEquals(LocalDate.parse("2026-11-30"), revenueDefault.period().endDate());

        var revenueSeries = useCase.query(
                MetricCode.PLATFORM_SUBSCRIPTION_REVENUE,
                new Scope("platform", ScopeType.PLATFORM),
                new Period(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-11-30")),
                TimeGranularity.MONTH);
        assertEquals(3, revenueSeries.points().size());
    }

    @Test
    void queriesStoreDailyMetricsByPredefinedWindowAndRejectsNonStoreWindowQueries() {
        var series7 = useCase.queryByWindow(
                MetricCode.STORE_PAGE_VISITS,
                new Scope("store-1", ScopeType.STORE),
                ComparisonWindow.DIAS_7);

        assertEquals(7, series7.points().size());
        assertEquals(LocalDate.parse("2026-11-11"), series7.period().startDate());
        assertEquals(LocalDate.parse("2026-11-17"), series7.period().endDate());

        assertThrows(InvalidTimeSeriesRequestException.class, () -> useCase.queryByWindow(
                MetricCode.PLATFORM_SEARCHES,
                new Scope("platform", ScopeType.PLATFORM),
                ComparisonWindow.DIAS_30));
    }

    private static final class RecordingMetricRepository implements MetricRepositoryPort {
        private List<Point> points = new ArrayList<>();
        private Period lastPeriod;
        private int calls;

        @Override
        public List<Point> findDailyPoints(MetricCode metricCode, Scope scope, Period period) {
            calls++;
            lastPeriod = period;
            return points;
        }

        @Override
        public Optional<BigDecimal> findGoalTarget(
                MetricCode metricCode,
                Scope scope,
                LocalDate periodStartDate
        ) {
            return Optional.empty();
        }

        @Override
        public List<GarmentSavesCount> findItemMetricTotals(MetricCode metricCode, Scope scope) {
            return List.of();
        }

        @Override
        public List<DistributionCount> findDistributionCounts(MetricCode metricCode, Scope scope) {
            return List.of();
        }
    }
}
