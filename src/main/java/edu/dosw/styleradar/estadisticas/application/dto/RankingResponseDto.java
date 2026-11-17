package edu.dosw.styleradar.estadisticas.application.dto;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;

import java.util.List;

public record RankingResponseDto(
        MetricCode metricCode,
        ScopeType scopeType,
        String scopeId,
        int limit,
        List<RankedItemDto> items,
        String message
) {
}
