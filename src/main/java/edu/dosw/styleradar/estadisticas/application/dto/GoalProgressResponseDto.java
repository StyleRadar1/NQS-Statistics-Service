package edu.dosw.styleradar.estadisticas.application.dto;

import edu.dosw.styleradar.estadisticas.domain.model.GoalProgressStatus;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;

import java.math.BigDecimal;

public record GoalProgressResponseDto(
        MetricCode metricCode,
        ScopeType scopeType,
        String scopeId,
        PeriodDto period,
        BigDecimal currentValue,
        BigDecimal targetValue,
        BigDecimal progressPercentage,
        GoalProgressStatus status,
        String message
) {
}
