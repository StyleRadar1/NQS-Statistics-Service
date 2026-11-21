package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.application.dto.GoalProgressResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.request.DefineGoalRequestDto;
import edu.dosw.styleradar.estadisticas.domain.model.GoalProgressStatus;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryGoalProgressUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics/goals")
@Tag(name = "Goal Progress", description = "Metric progress against weekly and monthly goals")
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
    @Operation(summary = "Query metric progress against its weekly or monthly goal")
    public ResponseEntity<GoalProgressResponseDto> query(@PathVariable MetricCode metricCode) {
        var scope = scopeResolver.resolve(metricCode);
        var result = queryGoalProgressUseCase.query(metricCode, scope);
        GoalProgressResponseDto dto = dtoMapper.toDto(result);
        if (result.status() == GoalProgressStatus.GOAL_NOT_DEFINED) {
            return ResponseEntity.unprocessableEntity().body(dto);
        }
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{metricCode}")
    @Operation(summary = "Define or update the weekly or monthly goal for a metric")
    public ResponseEntity<GoalProgressResponseDto> defineGoal(
            @PathVariable MetricCode metricCode,
            @Valid @RequestBody DefineGoalRequestDto request
    ) {
        var scope = scopeResolver.resolve(metricCode);
        var result = queryGoalProgressUseCase.defineGoal(metricCode, scope, request.targetValue());
        return ResponseEntity.ok(dtoMapper.toDto(result));
    }
}
