package edu.dosw.styleradar.estadisticas.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record MetricTotal(
        MetricCode metricCode,
        Scope scope,
        Period period,
        BigDecimal total
) {

    public MetricTotal {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Scope must not be null");
        Objects.requireNonNull(total, "Total must not be null");
        if (total.signum() < 0) {
            throw new IllegalArgumentException("Metric total must not be negative");
        }
    }
}
