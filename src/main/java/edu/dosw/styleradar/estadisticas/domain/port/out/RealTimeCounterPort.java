package edu.dosw.styleradar.estadisticas.domain.port.out;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.DailyMetricIncrement;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public interface RealTimeCounterPort {
    long countActive(MetricCode metricCode, Scope scope);

    default boolean markEventProcessed(String eventId, Duration ttl) {
        throw new UnsupportedOperationException("Event idempotency is not supported");
    }

    default void incrementActive(MetricCode metricCode, Scope scope, long amount) {
        throw new UnsupportedOperationException("Active counter increments are not supported");
    }

    default void adjustActive(MetricCode metricCode, Scope scope, long delta) {
        if (delta > 0) {
            incrementActive(metricCode, scope, delta);
        }
    }

    default void incrementDaily(
            MetricCode metricCode,
            Scope scope,
            LocalDate metricDate,
            BigDecimal amount
    ) {
        throw new UnsupportedOperationException("Daily counter increments are not supported");
    }

    default List<DailyMetricIncrement> drainDailyCounters() {
        throw new UnsupportedOperationException("Daily counter draining is not supported");
    }
}
