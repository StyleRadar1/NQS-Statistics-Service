package edu.dosw.styleradar.estadisticas.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record DailyMetricIncrement(
        MetricCode metricCode,
        Scope scope,
        LocalDate metricDate,
        BigDecimal value
) {

    public DailyMetricIncrement {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Scope must not be null");
        Objects.requireNonNull(metricDate, "Metric date must not be null");
        Objects.requireNonNull(value, "Increment value must not be null");
        if (metricCode.scopeType() != scope.type()) {
            throw new IllegalArgumentException("Metric code and scope types must match");
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Increment value must be positive");
        }
    }
}
