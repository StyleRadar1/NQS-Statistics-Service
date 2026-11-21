package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.application.dto.MetricSummaryResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.MetricTotalResponseDto;
import edu.dosw.styleradar.estadisticas.domain.model.ComparisonWindow;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryMetricSummaryUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics/summary")
@Tag(name = "Summary & Totals", description = "Indicators with percentage variation (SR-RF-76.1, SR-RF-76.2) and cumulative/monthly totals (SR-RF-82.1, SR-RF-86.4, SR-RF-86.5, SR-RF-86.6)")
public class MetricSummaryRestController {

    private final QueryMetricSummaryUseCase queryMetricSummaryUseCase;
    private final AuthenticatedScopeResolver scopeResolver;
    private final DtoMapper dtoMapper;

    public MetricSummaryRestController(
            QueryMetricSummaryUseCase queryMetricSummaryUseCase,
            AuthenticatedScopeResolver scopeResolver,
            DtoMapper dtoMapper
    ) {
        this.queryMetricSummaryUseCase = queryMetricSummaryUseCase;
        this.scopeResolver = scopeResolver;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping("/{metricCode}")
    @Operation(
            summary = "Query total store visits or received messages with percentage variation against the previous period",
            description = "Calculates the total for the selected period (7_DAYS, 30_DAYS, 90_DAYS) and its percentage variation "
                    + "compared to the immediately preceding period of equal duration.")
    public ResponseEntity<MetricSummaryResponseDto> query(
            @Parameter(description = "STORE_PAGE_VISITS or STORE_MESSAGES_RECEIVED", required = true)
            @PathVariable MetricCode metricCode,
            @Parameter(description = "Period to query: 7_DAYS, 30_DAYS, or 90_DAYS (also accepts 7_DIAS, 30_DIAS, 90_DIAS)", required = true)
            @RequestParam(name = "period", required = false) String period,
            @Parameter(hidden = true)
            @RequestParam(name = "periodo", required = false) String periodo
    ) throws org.springframework.web.bind.MissingServletRequestParameterException {
        String rawPeriod = (period != null && !period.isBlank()) ? period : periodo;
        if (rawPeriod == null || rawPeriod.isBlank()) {
            throw new org.springframework.web.bind.MissingServletRequestParameterException("period", "String");
        }
        ComparisonWindow window = ComparisonWindow.fromCode(rawPeriod);
        var scope = scopeResolver.resolve(metricCode);
        var result = queryMetricSummaryUseCase.queryWithVariation(metricCode, scope, window);
        return ResponseEntity.ok(dtoMapper.toDto(result));
    }

    @GetMapping("/totals/{metricCode}")
    @Operation(
            summary = "Query all-time cumulative total or current month total",
            description = "Returns the all-time total of garments received by foundation (FOUNDATION_GARMENTS_RECEIVED), "
                    + "published garments (PLATFORM_GARMENT_PUBLICATIONS), created playlists (PLATFORM_PLAYLISTS_CREATED), "
                    + "or current month donations (PLATFORM_MONTHLY_DONATIONS).")
    public ResponseEntity<MetricTotalResponseDto> queryTotal(
            @Parameter(description = "FOUNDATION_GARMENTS_RECEIVED, PLATFORM_GARMENT_PUBLICATIONS, PLATFORM_PLAYLISTS_CREATED, or PLATFORM_MONTHLY_DONATIONS", required = true)
            @PathVariable MetricCode metricCode
    ) {
        var scope = scopeResolver.resolve(metricCode);
        var result = queryMetricSummaryUseCase.queryTotal(metricCode, scope);
        return ResponseEntity.ok(dtoMapper.toDto(result));
    }
}
