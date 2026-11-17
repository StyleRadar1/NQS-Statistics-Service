package edu.dosw.styleradar.estadisticas.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record DistributionEntry(CategoryCode categoryCode, long count, BigDecimal percentage) {

    public DistributionEntry {
        Objects.requireNonNull(categoryCode, "Category code must not be null");
        Objects.requireNonNull(percentage, "Distribution percentage must not be null");
        if (count < 0) {
            throw new IllegalArgumentException("Distribution count must not be negative");
        }
        if (percentage.signum() < 0 || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Distribution percentage must be between 0 and 100");
        }
    }
}
