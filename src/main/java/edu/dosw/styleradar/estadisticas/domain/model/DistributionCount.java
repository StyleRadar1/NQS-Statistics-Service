package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.Objects;

public record DistributionCount(CategoryCode categoryCode, long count) {

    public DistributionCount {
        Objects.requireNonNull(categoryCode, "Category code must not be null");
        if (count < 0) {
            throw new IllegalArgumentException("Distribution count must not be negative");
        }
    }
}
