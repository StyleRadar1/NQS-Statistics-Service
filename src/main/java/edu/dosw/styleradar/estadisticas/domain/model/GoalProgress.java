package edu.dosw.styleradar.estadisticas.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record GoalProgress(
        MetricCode metricCode,
        Scope scope,
        Period period,
        BigDecimal currentValue,
        BigDecimal targetValue,
        BigDecimal progressPercentage,
        GoalProgressStatus status
) {

    public GoalProgress {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Scope must not be null");
        Objects.requireNonNull(period, "Period must not be null");
        Objects.requireNonNull(currentValue, "Current value must not be null");
        Objects.requireNonNull(status, "Goal progress status must not be null");

        if (status == GoalProgressStatus.GOAL_NOT_DEFINED) {
            if (targetValue != null || progressPercentage != null) {
                throw new IllegalArgumentException("Undefined goals must not have a target or progress percentage");
            }
        } else {
            Objects.requireNonNull(targetValue, "Target value must not be null");
            Objects.requireNonNull(progressPercentage, "Progress percentage must not be null");
            if (targetValue.signum() <= 0) {
                throw new IllegalArgumentException("Target value must be positive");
            }
        }
    }

}
