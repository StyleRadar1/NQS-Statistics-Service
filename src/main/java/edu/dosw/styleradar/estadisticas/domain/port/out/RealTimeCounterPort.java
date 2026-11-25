package edu.dosw.styleradar.estadisticas.domain.port.out;

import edu.dosw.styleradar.estadisticas.domain.model.DailyMetricIncrement;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public interface RealTimeCounterPort {
    default boolean markEventProcessed(String eventId, Duration ttl) {
        throw new UnsupportedOperationException("Event idempotency is not supported");
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
