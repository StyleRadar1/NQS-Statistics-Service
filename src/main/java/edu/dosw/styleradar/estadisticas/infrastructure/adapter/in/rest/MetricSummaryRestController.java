package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.application.dto.MetricTotalResponseDto;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryMetricSummaryUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics/summary")
@Tag(name = "Totals", description = "Cumulative and monthly total indicators (SR-RF-82.1, SR-RF-86.4, SR-RF-86.5, SR-RF-86.6)")
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

    @GetMapping("/totals/{metricCode}")
    @Operation(
            summary = "Query all-time cumulative total or current month total",
            description = "Returns the all-time total of garments received by foundation (FOUNDATION_GARMENTS_RECEIVED), "
                    + "published garments (PLATFORM_GARMENT_PUBLICATIONS), created playlists (PLATFORM_PLAYLISTS_CREATED), "
                    + "or current month donations (PLATFORM_MONTHLY_DONATIONS).")
    public ResponseEntity<MetricTotalResponseDto> queryTotal(
            @Parameter(
                    description = "FOUNDATION_GARMENTS_RECEIVED (SR-RF-82.1), PLATFORM_GARMENT_PUBLICATIONS (SR-RF-86.4), "
                            + "PLATFORM_PLAYLISTS_CREATED (SR-RF-86.5), or PLATFORM_MONTHLY_DONATIONS (SR-RF-86.6)",
                    required = true,
                    schema = @Schema(allowableValues = {
                            "FOUNDATION_GARMENTS_RECEIVED",
                            "PLATFORM_GARMENT_PUBLICATIONS",
                            "PLATFORM_PLAYLISTS_CREATED",
                            "PLATFORM_MONTHLY_DONATIONS"}))
            @PathVariable MetricCode metricCode
    ) {
        var scope = scopeResolver.resolve(metricCode);
        var result = queryMetricSummaryUseCase.queryTotal(metricCode, scope);
        return ResponseEntity.ok(dtoMapper.toDto(result));
    }
}
