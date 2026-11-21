package edu.dosw.styleradar.estadisticas.application.dto;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.model.VariationStatus;

import java.math.BigDecimal;

public record MetricSummaryResponseDto(
        MetricCode metricCode,
        ScopeType scopeType,
        String scopeId,
        String periodo,
        PeriodDto currentPeriod,
        PeriodDto previousPeriod,
        BigDecimal total,
        BigDecimal previousTotal,
        BigDecimal variationPercentage,
        VariationStatus variationStatus,
        String indicatorMessage
) {
}
