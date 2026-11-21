package edu.dosw.styleradar.estadisticas.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record MetricSummaryWithVariation(
        MetricCode metricCode,
        Scope scope,
        ComparisonWindow window,
        Period currentPeriod,
        Period previousPeriod,
        BigDecimal currentTotal,
        BigDecimal previousTotal,
        BigDecimal variationPercentage,
        VariationStatus variationStatus,
        String indicatorMessage
) {

    public MetricSummaryWithVariation {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Scope must not be null");
        Objects.requireNonNull(window, "Comparison window must not be null");
        Objects.requireNonNull(currentPeriod, "Current period must not be null");
        Objects.requireNonNull(previousPeriod, "Previous period must not be null");
        Objects.requireNonNull(currentTotal, "Current total must not be null");
        Objects.requireNonNull(previousTotal, "Previous total must not be null");
        Objects.requireNonNull(variationStatus, "Variation status must not be null");
        if (currentTotal.signum() < 0 || previousTotal.signum() < 0) {
            throw new IllegalArgumentException("Metric totals must not be negative");
        }
        if (variationStatus == VariationStatus.NOT_CALCULABLE_PREVIOUS_ZERO && variationPercentage != null) {
            throw new IllegalArgumentException("Uncomputable variation must not include a percentage");
        }
        if (variationStatus != VariationStatus.NOT_CALCULABLE_PREVIOUS_ZERO && variationPercentage == null) {
            throw new IllegalArgumentException("Calculated variation must include a percentage");
        }
    }
}
