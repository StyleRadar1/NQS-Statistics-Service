package edu.dosw.styleradar.estadisticas.application.dto;

import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Cumulative all-time or current month total for a specific indicator (SR-RF-82.1, SR-RF-86.4, SR-RF-86.5, SR-RF-86.6)")
public record MetricTotalResponseDto(
        @Schema(description = "Queried metric code", example = "FOUNDATION_GARMENTS_RECEIVED")
        MetricCode metricCode,
        @Schema(description = "Authenticated scope type", example = "FOUNDATION")
        ScopeType scopeType,
        @Schema(description = "Authenticated scope identifier", example = "foundation-1")
        String scopeId,
        @Schema(description = "Covered period when applicable (null for all-time cumulative total)")
        PeriodDto period,
        @Schema(description = "Calculated total value", example = "128")
        BigDecimal total
) {
}
