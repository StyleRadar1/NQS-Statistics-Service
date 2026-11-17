package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.List;
import java.util.Objects;

public record Ranking(
        MetricCode metricCode,
        Scope scope,
        int limit,
        List<RankedItem> items,
        String message
) {

    public Ranking {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Scope must not be null");
        if (limit < 1) {
            throw new IllegalArgumentException("Ranking limit must be positive");
        }
        items = List.copyOf(Objects.requireNonNull(items, "Ranking items must not be null"));
        if (items.size() > limit) {
            throw new IllegalArgumentException("Ranking items must not exceed the limit");
        }
        if (items.isEmpty() && (message == null || message.isBlank())) {
            throw new IllegalArgumentException("An empty ranking must include a message");
        }
    }
}
