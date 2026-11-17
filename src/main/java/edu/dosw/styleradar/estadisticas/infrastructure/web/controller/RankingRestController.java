package edu.dosw.styleradar.estadisticas.infrastructure.web.controller;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.application.dto.RankingResponseDto;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryRankingUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics/rankings")
public class RankingRestController {

    private final QueryRankingUseCase queryRankingUseCase;
    private final AuthenticatedScopeResolver scopeResolver;
    private final DtoMapper dtoMapper;

    public RankingRestController(
            QueryRankingUseCase queryRankingUseCase,
            AuthenticatedScopeResolver scopeResolver,
            DtoMapper dtoMapper
    ) {
        this.queryRankingUseCase = queryRankingUseCase;
        this.scopeResolver = scopeResolver;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping("/{metricCode}")
    @Operation(summary = "Query the six garments with the most wishlist saves")
    public ResponseEntity<RankingResponseDto> query(@PathVariable MetricCode metricCode) {
        var scope = scopeResolver.resolve(metricCode);
        var ranking = queryRankingUseCase.query(metricCode, scope);
        return ResponseEntity.ok(dtoMapper.toDto(ranking));
    }
}
