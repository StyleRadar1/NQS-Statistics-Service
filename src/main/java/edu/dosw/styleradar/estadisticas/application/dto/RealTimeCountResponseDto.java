package edu.dosw.styleradar.estadisticas.application.dto;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;

public record RealTimeCountResponseDto(
        MetricCode metricCode,
        ScopeType scopeType,
        String scopeId,
        long activeCount
) {
}
