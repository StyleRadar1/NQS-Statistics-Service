package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.GarmentSavesCount;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryRankingUseCaseTest {

    private static final Scope STORE_SCOPE = new Scope("store-17", ScopeType.STORE);

    private final RecordingMetricRepository repository = new RecordingMetricRepository();
    private QueryRankingUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repository.items = List.of();
        repository.itemMetricCalls = 0;
        useCase = new QueryRankingUseCaseImpl(repository);
    }

    @Test
    void returnsTheSixMostSavedItemsInDescendingOrder() {
        repository.items = List.of(
                item("garment-1", 10),
                item("garment-2", 100),
                item("garment-3", 40),
                item("garment-4", 90),
                item("garment-5", 30),
                item("garment-6", 80),
                item("garment-7", 20),
                item("garment-8", 70),
                item("garment-9", 60),
                item("garment-10", 50));

        var result = useCase.query(MetricCode.STORE_GARMENTS_BY_SAVES, STORE_SCOPE);

        assertEquals(6, result.items().size());
        assertEquals(
                List.of("garment-2", "garment-4", "garment-6", "garment-8", "garment-9", "garment-10"),
                result.items().stream().map(rankedItem -> rankedItem.itemId()).toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 6),
                result.items().stream().map(rankedItem -> rankedItem.position()).toList());
        assertEquals(List.of(100L, 90L, 80L, 70L, 60L, 50L),
                result.items().stream().map(rankedItem -> rankedItem.savesCount()).toList());
        assertEquals(MetricCode.STORE_GARMENTS_BY_SAVES, repository.requestedMetric);
        assertEquals(STORE_SCOPE, repository.requestedScope);
    }

    @Test
    void returnsAllItemsWhenFewerThanSixArePublished() {
        repository.items = List.of(item("garment-1", 8), item("garment-2", 13), item("garment-3", 5));

        var result = useCase.query(MetricCode.STORE_GARMENTS_BY_SAVES, STORE_SCOPE);

        assertEquals(3, result.items().size());
        assertEquals(List.of("garment-2", "garment-1", "garment-3"),
                result.items().stream().map(rankedItem -> rankedItem.itemId()).toList());
        assertEquals(List.of(13L, 8L, 5L),
                result.items().stream().map(rankedItem -> rankedItem.savesCount()).toList());
    }

    @Test
    void returnsAnEmptyRankingAndMessageWhenNoGarmentsArePublished() {
        var result = useCase.query(MetricCode.STORE_GARMENTS_BY_SAVES, STORE_SCOPE);

        assertEquals(List.of(), result.items());
        assertEquals("No hay prendas publicadas", result.message());
        assertEquals(1, repository.itemMetricCalls);
    }

    @Test
    void rejectsNonStoreScopesBeforeQueryingRepository() {
        assertThrows(UnauthorizedScopeException.class, () -> useCase.query(
                MetricCode.STORE_GARMENTS_BY_SAVES,
                new Scope("platform", ScopeType.PLATFORM)));

        assertEquals(0, repository.itemMetricCalls);
    }

    @Test
    void rejectsMetricsThatDoNotSupportGarmentRankings() {
        assertThrows(UnsupportedMetricScopeException.class, () -> useCase.query(
                MetricCode.STORE_WISHLIST_SAVES,
                STORE_SCOPE));

        assertEquals(0, repository.itemMetricCalls);
    }

    private GarmentSavesCount item(String itemId, long savesCount) {
        return new GarmentSavesCount(itemId, "Name " + itemId, savesCount);
    }

    private static final class RecordingMetricRepository implements MetricRepositoryPort {
        private List<GarmentSavesCount> items = List.of();
        private MetricCode requestedMetric;
        private Scope requestedScope;
        private int itemMetricCalls;

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
            itemMetricCalls++;
            requestedMetric = metricCode;
            requestedScope = scope;
            return items;
        }

        @Override
        public List<DistributionCount> findDistributionCounts(MetricCode metricCode, Scope scope) {
            return List.of();
        }
    }
}
