package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DistributionCount;
import edu.dosw.styleradar.estadisticas.domain.model.GarmentSavesCount;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Period;
import edu.dosw.styleradar.estadisticas.domain.model.Point;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.out.MetricRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryDistributionUseCaseTest {

    private static final Scope STORE_SCOPE = new Scope("store-17", ScopeType.STORE);
    private static final Scope FOUNDATION_SCOPE = new Scope("foundation-42", ScopeType.FOUNDATION);
    private static final Scope PLATFORM_SCOPE = new Scope("platform", ScopeType.PLATFORM);

    private final RecordingMetricRepository repository = new RecordingMetricRepository();
    private QueryDistributionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repository.counts = List.of();
        repository.distributionCalls = 0;
        useCase = new QueryDistributionUseCaseImpl(repository);
    }

    @Test
    void calculatesStoreInventoryStateCountsAndPercentages() {
        repository.counts = List.of(
                count("AVAILABLE", 5),
                count("LOW_STOCK", 2),
                count("OUT_OF_STOCK", 3));

        var result = useCase.query(MetricCode.STORE_INVENTORY_BY_STATE, STORE_SCOPE);

        assertEquals(MetricCode.STORE_INVENTORY_BY_STATE, result.metricCode());
        assertEquals(STORE_SCOPE, result.scope());
        assertEntry(result.entries().get(0), "AVAILABLE", 5, "50.0");
        assertEntry(result.entries().get(1), "LOW_STOCK", 2, "20.0");
        assertEntry(result.entries().get(2), "OUT_OF_STOCK", 3, "30.0");
        assertRequested(MetricCode.STORE_INVENTORY_BY_STATE, STORE_SCOPE);
    }

    @Test
    void calculatesFoundationGarmentTypeCountsAndPercentagesForAuthenticatedFoundation() {
        repository.counts = List.of(count("TOP", 3), count("DRESS", 2));

        var result = useCase.query(MetricCode.FOUNDATION_GARMENTS_BY_TYPE, FOUNDATION_SCOPE);

        assertEquals(2, result.entries().size());
        assertEntry(result.entries().get(0), "DRESS", 2, "40.0");
        assertEntry(result.entries().get(1), "TOP", 3, "60.0");
        assertRequested(MetricCode.FOUNDATION_GARMENTS_BY_TYPE, FOUNDATION_SCOPE);
    }

    @Test
    void calculatesFoundationRequestStateCountsAndPercentagesForAuthenticatedFoundation() {
        repository.counts = List.of(count("PENDING", 1), count("APPROVED", 3));

        var result = useCase.query(MetricCode.FOUNDATION_REQUESTS_BY_STATE, FOUNDATION_SCOPE);

        assertEntry(result.entries().get(0), "APPROVED", 3, "75.0");
        assertEntry(result.entries().get(1), "PENDING", 1, "25.0");
        assertRequested(MetricCode.FOUNDATION_REQUESTS_BY_STATE, FOUNDATION_SCOPE);
    }

    @Test
    void calculatesPlatformBuyerPlanCountsAndPercentages() {
        repository.counts = List.of(count("PLAN_RADAR", 4), count("PLAN_RADAR_PLUS", 1));

        var result = useCase.query(MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM_SCOPE);

        assertEntry(result.entries().get(0), "PLAN_RADAR", 4, "80.0");
        assertEntry(result.entries().get(1), "PLAN_RADAR_PLUS", 1, "20.0");
        assertRequested(MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM_SCOPE);
    }

    @Test
    void calculatesPlatformStorePlanCountsAndPercentages() {
        repository.counts = List.of(count("PLAN_RADAR_PLUS", 5));

        var result = useCase.query(MetricCode.PLATFORM_STORES_BY_PLAN, PLATFORM_SCOPE);

        assertEntry(result.entries().get(0), "PLAN_RADAR", 0, "0.0");
        assertEntry(result.entries().get(1), "PLAN_RADAR_PLUS", 5, "100.0");
        assertRequested(MetricCode.PLATFORM_STORES_BY_PLAN, PLATFORM_SCOPE);
    }

    @Test
    void returnsAllInventoryStatesAtZeroWhenThereAreNoGarments() {
        var result = useCase.query(MetricCode.STORE_INVENTORY_BY_STATE, STORE_SCOPE);

        assertEquals(3, result.entries().size());
        assertEntry(result.entries().get(0), "AVAILABLE", 0, "0.0");
        assertEntry(result.entries().get(1), "LOW_STOCK", 0, "0.0");
        assertEntry(result.entries().get(2), "OUT_OF_STOCK", 0, "0.0");
    }

    @Test
    void returnsEmptyFoundationDistributionWhenThereAreNoRecords() {
        for (MetricCode metricCode : List.of(
                MetricCode.FOUNDATION_GARMENTS_BY_TYPE,
                MetricCode.FOUNDATION_REQUESTS_BY_STATE)) {
            var result = useCase.query(metricCode, FOUNDATION_SCOPE);

            assertEquals(List.of(), result.entries());
            assertRequested(metricCode, FOUNDATION_SCOPE);
        }
    }

    @Test
    void returnsBothPlansAtZeroWhenThereAreNoAccounts() {
        var result = useCase.query(MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM_SCOPE);

        assertEntry(result.entries().get(0), "PLAN_RADAR", 0, "0.0");
        assertEntry(result.entries().get(1), "PLAN_RADAR_PLUS", 0, "0.0");
    }

    @Test
    void rejectsScopesThatDoNotOwnTheRequestedMetric() {
        assertThrows(UnauthorizedScopeException.class, () -> useCase.query(
                MetricCode.STORE_INVENTORY_BY_STATE,
                FOUNDATION_SCOPE));

        assertEquals(0, repository.distributionCalls);
    }

    @Test
    void rejectsUnsupportedMetricsAndUnsupportedCategoriesInDistribution() {
        assertThrows(UnsupportedMetricScopeException.class, () -> useCase.query(
                MetricCode.STORE_PAGE_VISITS,
                STORE_SCOPE));

        repository.counts = List.of(count("UNSUPPORTED_CATEGORY", 1));
        assertThrows(IllegalStateException.class, () -> useCase.query(
                MetricCode.STORE_INVENTORY_BY_STATE,
                STORE_SCOPE));
    }

    private DistributionCount count(String category, long value) {
        return new DistributionCount(new CategoryCode(category), value);
    }

    private void assertRequested(MetricCode metricCode, Scope scope) {
        assertEquals(metricCode, repository.requestedMetric);
        assertEquals(scope, repository.requestedScope);
    }

    private void assertEntry(
            edu.dosw.styleradar.estadisticas.domain.model.DistributionEntry entry,
            String category,
            long count,
            String percentage
    ) {
        assertEquals(category, entry.categoryCode().value());
        assertEquals(count, entry.count());
        assertEquals(0, entry.percentage().compareTo(new BigDecimal(percentage)));
    }

    private static final class RecordingMetricRepository implements MetricRepositoryPort {
        private List<DistributionCount> counts = List.of();
        private MetricCode requestedMetric;
        private Scope requestedScope;
        private int distributionCalls;

        @Override
        public List<Point> findDailyPoints(MetricCode metricCode, Scope scope, Period period) {
            return List.of();
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
            distributionCalls++;
            requestedMetric = metricCode;
            requestedScope = scope;
            return counts;
        }
    }
}
