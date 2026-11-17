package edu.dosw.styleradar.estadisticas.application.dto;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;

import java.util.List;

public record DistributionResponseDto(
        MetricCode metricCode,
        ScopeType scopeType,
        String scopeId,
        List<DistributionEntryDto> entries
) {
    public DistributionResponseDto {
        entries = List.copyOf(entries);
    }
}
