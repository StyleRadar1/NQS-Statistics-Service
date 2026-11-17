package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.Objects;

public record RealTimeCount(MetricCode metricCode, Scope scope, long activeCount) {

    public RealTimeCount {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Scope must not be null");
        if (activeCount < 0) {
            throw new IllegalArgumentException("Active count must not be negative");
        }
    }
}
