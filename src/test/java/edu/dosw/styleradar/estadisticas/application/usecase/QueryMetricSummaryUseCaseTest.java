package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.ComparisonWindow;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.GarmentSavesCount;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.model.VariationStatus;
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
import java.util.Optional;

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
    void calculatesPositiveVariationForStoreVisitsOver7Days() {
        LocalDate today = LocalDate.parse("2026-11-21");
        Period current = ComparisonWindow.DIAS_7.currentPeriod(today);
        Period previous = ComparisonWindow.DIAS_7.previousPeriod(today);

        repository.put(current, List.of(
                new Point(LocalDate.parse("2026-11-15"), BigDecimal.valueOf(80)),
                new Point(LocalDate.parse("2026-11-21"), BigDecimal.valueOf(70))));
        repository.put(previous, List.of(
                new Point(LocalDate.parse("2026-11-10"), BigDecimal.valueOf(100))));

        var result = useCase.queryWithVariation(
                MetricCode.STORE_PAGE_VISITS, STORE_SCOPE, ComparisonWindow.DIAS_7);

        assertEquals(new Period(LocalDate.parse("2026-11-15"), LocalDate.parse("2026-11-21")), result.currentPeriod());
        assertEquals(new Period(LocalDate.parse("2026-11-08"), LocalDate.parse("2026-11-14")), result.previousPeriod());
        assertEquals(0, new BigDecimal("150").compareTo(result.currentTotal()));
        assertEquals(0, new BigDecimal("100").compareTo(result.previousTotal()));
        assertEquals(new BigDecimal("50.00"), result.variationPercentage());
        assertEquals(VariationStatus.CALCULATED, result.variationStatus());
        assertNull(result.indicatorMessage());
    }

    @Test
    void returnsNegative100PercentWhenCurrentPeriodIsZeroAndPreviousHadActivity() {
        LocalDate today = LocalDate.parse("2026-11-21");
        Period previous = ComparisonWindow.DIAS_30.previousPeriod(today);
        repository.put(previous, List.of(new Point(LocalDate.parse("2026-10-10"), BigDecimal.valueOf(40))));

        var result = useCase.queryWithVariation(
                MetricCode.STORE_PAGE_VISITS, STORE_SCOPE, ComparisonWindow.DIAS_30);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.currentTotal()));
        assertEquals(new BigDecimal("-100.00"), result.variationPercentage());
        assertEquals(VariationStatus.CALCULATED, result.variationStatus());
        assertNull(result.indicatorMessage());
    }

    @Test
    void returnsNoVariationIndicatorWhenBothPeriodsAreZero() {
        var visits = useCase.queryWithVariation(
                MetricCode.STORE_PAGE_VISITS, STORE_SCOPE, ComparisonWindow.DIAS_7);
        assertEquals(VariationStatus.NO_VARIATION_BOTH_ZERO, visits.variationStatus());
        assertEquals(new BigDecimal("0.00"), visits.variationPercentage());
        assertEquals("Sin variación: ambos periodos sin visitas", visits.indicatorMessage());

        var messages = useCase.queryWithVariation(
                MetricCode.STORE_MESSAGES_RECEIVED, STORE_SCOPE, ComparisonWindow.DIAS_90);
        assertEquals(VariationStatus.NO_VARIATION_BOTH_ZERO, messages.variationStatus());
        assertEquals("Sin variación: ambos periodos sin mensajes", messages.indicatorMessage());
    }

    @Test
    void returnsNotCalculableIndicatorWhenPreviousPeriodIsZeroAndCurrentIsPositive() {
        LocalDate today = LocalDate.parse("2026-11-21");
        Period current = ComparisonWindow.DIAS_7.currentPeriod(today);
        repository.put(current, List.of(new Point(today, BigDecimal.valueOf(12))));

        var visits = useCase.queryWithVariation(
                MetricCode.STORE_PAGE_VISITS, STORE_SCOPE, ComparisonWindow.DIAS_7);
        assertEquals(VariationStatus.NOT_CALCULABLE_PREVIOUS_ZERO, visits.variationStatus());
        assertNull(visits.variationPercentage());
        assertEquals("No calculable: periodo anterior con 0 visitas", visits.indicatorMessage());

        var messages = useCase.queryWithVariation(
                MetricCode.STORE_MESSAGES_RECEIVED, STORE_SCOPE, ComparisonWindow.DIAS_7);
        assertEquals(VariationStatus.NOT_CALCULABLE_PREVIOUS_ZERO, messages.variationStatus());
        assertNull(messages.variationPercentage());
        assertEquals("No calculable: periodo anterior con 0 mensajes", messages.indicatorMessage());
    }

    @Test
    void rejectsUnauthorizedScopeAndUnsupportedMetric() {
        assertThrows(UnauthorizedScopeException.class, () -> useCase.queryWithVariation(
                MetricCode.STORE_PAGE_VISITS,
                new Scope("platform", ScopeType.PLATFORM),
                ComparisonWindow.DIAS_7));

        assertThrows(UnsupportedMetricScopeException.class, () -> useCase.queryWithVariation(
                MetricCode.STORE_INVENTORY_BY_STATE,
                STORE_SCOPE,
                ComparisonWindow.DIAS_7));
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
                MetricCode.STORE_PAGE_VISITS, STORE_SCOPE));
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
        public Optional<BigDecimal> findGoalTarget(MetricCode metricCode, Scope scope, LocalDate periodStartDate) {
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
