package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.application.dto.GoalProgressResponseDto;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryGoalProgressUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics/goals")
@Tag(name = "Goal progress", description = "Weekly and monthly goal progress")
public class GoalProgressRestController {

    private final QueryGoalProgressUseCase queryGoalProgressUseCase;
    private final AuthenticatedScopeResolver scopeResolver;
    private final DtoMapper dtoMapper;

    public GoalProgressRestController(
            QueryGoalProgressUseCase queryGoalProgressUseCase,
            AuthenticatedScopeResolver scopeResolver,
            DtoMapper dtoMapper
    ) {
        this.queryGoalProgressUseCase = queryGoalProgressUseCase;
        this.scopeResolver = scopeResolver;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping("/{metricCode}")
    @Operation(summary = "Query weekly or monthly goal progress")
    public ResponseEntity<GoalProgressResponseDto> query(@PathVariable MetricCode metricCode) {
        var scope = scopeResolver.resolve(metricCode);
        var result = queryGoalProgressUseCase.query(metricCode, scope);
        return ResponseEntity.ok(dtoMapper.toDto(result));
    }
}
