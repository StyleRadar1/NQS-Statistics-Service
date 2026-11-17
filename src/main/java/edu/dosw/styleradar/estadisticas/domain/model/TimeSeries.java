package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.List;
import java.util.Objects;

public record TimeSeries(
        MetricCode metricCode,
        Scope scope,
        Period period,
        TimeGranularity granularity,
        List<Point> points
) {

    public TimeSeries {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Scope must not be null");
        Objects.requireNonNull(period, "Period must not be null");
        Objects.requireNonNull(granularity, "Granularity must not be null");
        points = List.copyOf(Objects.requireNonNull(points, "Points must not be null"));
    }
}
