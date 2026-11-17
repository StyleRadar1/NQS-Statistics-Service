package edu.dosw.styleradar.estadisticas.application.dto;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.model.TimeGranularity;

import java.util.List;

public record TimeSeriesResponseDto(
        MetricCode metricCode,
        ScopeType scopeType,
        String scopeId,
        PeriodDto period,
        TimeGranularity granularity,
        List<PointDto> points
) {
}
