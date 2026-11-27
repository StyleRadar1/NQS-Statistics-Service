package edu.dosw.styleradar.estadisticas.infrastructure.web.controller;

import edu.dosw.styleradar.estadisticas.application.dto.DistributionResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryDistributionUseCase;
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
@RequestMapping("/statistics/distributions")
@Tag(name = "Distributions", description = "Category count and percentage distributions")
public class DistributionRestController {

    private final QueryDistributionUseCase queryDistributionUseCase;
    private final AuthenticatedScopeResolver scopeResolver;
    private final DtoMapper dtoMapper;

    public DistributionRestController(
            QueryDistributionUseCase queryDistributionUseCase,
            AuthenticatedScopeResolver scopeResolver,
            DtoMapper dtoMapper
    ) {
        this.queryDistributionUseCase = queryDistributionUseCase;
        this.scopeResolver = scopeResolver;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping("/{metricCode}")
    @Operation(summary = "Query the category and percentage distribution for a metric")
    public ResponseEntity<DistributionResponseDto> query(
            @Parameter(
                    description = "STORE_INVENTORY_BY_STATE (store inventory items by stock state), "
                            + "PLATFORM_BUYERS_BY_PLAN (buyer accounts by subscription plan), or "
                            + "PLATFORM_STORES_BY_PLAN (store accounts by subscription status)",
                    required = true,
                    schema = @Schema(allowableValues = {
                            "STORE_INVENTORY_BY_STATE",
                            "PLATFORM_BUYERS_BY_PLAN",
                            "PLATFORM_STORES_BY_PLAN"}))
            @PathVariable MetricCode metricCode
    ) {
        var scope = scopeResolver.resolve(metricCode);
        var distribution = queryDistributionUseCase.query(metricCode, scope);
        return ResponseEntity.ok(dtoMapper.toDto(distribution));
    }
}
