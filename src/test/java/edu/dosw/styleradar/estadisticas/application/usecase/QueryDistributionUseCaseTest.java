package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryDistributionUseCaseTest {

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
    void calculatesPlatformBuyerPlanCountsAndPercentages() {
        repository.counts = List.of(count("PLAN_RADAR", 4), count("PLAN_RADAR_PLUS", 1));

        var result = useCase.query(MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM_SCOPE);

        assertEntry(result.entries().get(0), "PLAN_RADAR", 4, "80.0");
        assertEntry(result.entries().get(1), "PLAN_RADAR_PLUS", 1, "20.0");
        assertRequested(MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM_SCOPE);
    }

    @Test
    void calculatesPlatformStorePlanCountsAndPercentages() {
        repository.counts = List.of(count("PLAN_STORE_MONTHLY", 4), count("SUSPENDED", 1));

        var result = useCase.query(MetricCode.PLATFORM_STORES_BY_PLAN, PLATFORM_SCOPE);

        assertEquals(2, result.entries().size());
        assertEntry(result.entries().get(0), "PLAN_STORE_MONTHLY", 4, "80.00");
        assertEntry(result.entries().get(1), "SUSPENDED", 1, "20.00");
        assertRequested(MetricCode.PLATFORM_STORES_BY_PLAN, PLATFORM_SCOPE);

        repository.counts = List.of();
        var emptyResult = useCase.query(MetricCode.PLATFORM_STORES_BY_PLAN, PLATFORM_SCOPE);
        assertEntry(emptyResult.entries().get(0), "PLAN_STORE_MONTHLY", 0, "0.00");
        assertEntry(emptyResult.entries().get(1), "SUSPENDED", 0, "0.00");
    }

    @Test
    void returnsAllBuyerPlansAtZeroWhenEmpty() {
        var buyers = useCase.query(MetricCode.PLATFORM_BUYERS_BY_PLAN, PLATFORM_SCOPE);
        assertEquals(2, buyers.entries().size());
        assertEntry(buyers.entries().get(0), "PLAN_RADAR", 0, "0.0");
        assertEntry(buyers.entries().get(1), "PLAN_RADAR_PLUS", 0, "0.0");
    }

    @Test
    void rejectsUnauthorizedScopesUnsupportedMetricsAndUnsupportedCategories() {
        assertThrows(UnauthorizedScopeException.class, () -> useCase.query(
                MetricCode.PLATFORM_BUYERS_BY_PLAN,
                FOUNDATION_SCOPE));

        assertEquals(0, repository.distributionCalls);

        assertThrows(UnsupportedMetricScopeException.class, () -> useCase.query(
                MetricCode.PLATFORM_PLAYLISTS_CREATED,
                PLATFORM_SCOPE));

        repository.counts = List.of(count("UNSUPPORTED_CATEGORY", 1));
        assertThrows(IllegalStateException.class, () -> useCase.query(
                MetricCode.PLATFORM_BUYERS_BY_PLAN,
                PLATFORM_SCOPE));
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
        public List<DistributionCount> findDistributionCounts(MetricCode metricCode, Scope scope) {
            distributionCalls++;
            requestedMetric = metricCode;
            requestedScope = scope;
            return counts;
        }
    }
}
