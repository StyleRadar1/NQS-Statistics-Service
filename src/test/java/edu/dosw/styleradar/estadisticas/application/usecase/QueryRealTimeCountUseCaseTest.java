package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.out.RealTimeCounterPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryRealTimeCountUseCaseTest {

    private final RecordingRealTimeCounter counter = new RecordingRealTimeCounter();
    private QueryRealTimeCountUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new QueryRealTimeCountUseCaseImpl(counter);
        counter.activeCount = 0;
        counter.calls = 0;
    }

    @Test
    void returnsActiveVisitorsForAuthenticatedStore() {
        counter.activeCount = 7;
        Scope scope = new Scope("store-17", ScopeType.STORE);

        var result = useCase.query(MetricCode.STORE_REALTIME_VISITORS, scope);

        assertEquals(7, result.activeCount());
        assertEquals(MetricCode.STORE_REALTIME_VISITORS, counter.lastMetricCode);
        assertEquals(scope, counter.lastScope);
        assertEquals(1, counter.calls);
    }

    @Test
    void returnsActiveUsersAndFoundationsForPlatformMetrics() {
        Scope scope = new Scope("platform", ScopeType.PLATFORM);
        counter.activeCount = 12;

        for (MetricCode metricCode : new MetricCode[] {
                MetricCode.PLATFORM_REALTIME_USERS,
                MetricCode.PLATFORM_ACTIVE_USERS,
                MetricCode.PLATFORM_ACTIVE_FOUNDATIONS
        }) {
            var result = useCase.query(metricCode, scope);

            assertEquals(12, result.activeCount(), metricCode.name());
            assertEquals(metricCode, result.metricCode());
        }

        assertEquals(3, counter.calls);
        assertEquals(scope, counter.lastScope);
    }

    @Test
    void returnsZeroWhenNobodyIsActive() {
        var result = useCase.query(
                MetricCode.STORE_REALTIME_VISITORS,
                new Scope("store-17", ScopeType.STORE));

        assertEquals(0, result.activeCount());
        assertEquals(1, counter.calls);
    }

    @Test
    void rejectsUnauthorizedScopeBeforeQueryingCounter() {
        assertThrows(UnauthorizedScopeException.class, () -> useCase.query(
                MetricCode.STORE_REALTIME_VISITORS,
                new Scope("platform", ScopeType.PLATFORM)));

        assertEquals(0, counter.calls);
    }

    @Test
    void rejectsMetricsThatDoNotSupportRealTimeCounts() {
        assertThrows(UnsupportedMetricScopeException.class, () -> useCase.query(
                MetricCode.STORE_PAGE_VISITS,
                new Scope("store-17", ScopeType.STORE)));

        assertEquals(0, counter.calls);
    }

    private static final class RecordingRealTimeCounter implements RealTimeCounterPort {
        private long activeCount;
        private MetricCode lastMetricCode;
        private Scope lastScope;
        private int calls;

        @Override
        public long countActive(MetricCode metricCode, Scope scope) {
            calls++;
            lastMetricCode = metricCode;
            lastScope = scope;
            return activeCount;
        }
    }
}
