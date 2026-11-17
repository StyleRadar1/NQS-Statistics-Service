package edu.dosw.styleradar.estadisticas.infrastructure.web.controller;

import edu.dosw.styleradar.estadisticas.application.dto.DistributionResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryDistributionUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics/distributions")
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
    public ResponseEntity<DistributionResponseDto> query(@PathVariable MetricCode metricCode) {
        var scope = scopeResolver.resolve(metricCode);
        var distribution = queryDistributionUseCase.query(metricCode, scope);
        return ResponseEntity.ok(dtoMapper.toDto(distribution));
    }
}
