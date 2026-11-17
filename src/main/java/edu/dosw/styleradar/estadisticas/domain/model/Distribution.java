package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record Distribution(MetricCode metricCode, Scope scope, List<DistributionEntry> entries) {

    public Distribution {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Scope must not be null");
        entries = List.copyOf(Objects.requireNonNull(entries, "Distribution entries must not be null"));
        var categoryCodes = new HashSet<CategoryCode>();
        if (entries.stream().anyMatch(entry -> !categoryCodes.add(entry.categoryCode()))) {
            throw new IllegalArgumentException("Distribution categories must be unique");
        }
    }
}
