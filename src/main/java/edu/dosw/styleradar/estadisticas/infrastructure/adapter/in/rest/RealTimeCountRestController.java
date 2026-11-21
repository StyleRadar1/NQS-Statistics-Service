package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.DtoMapper;
import edu.dosw.styleradar.estadisticas.application.dto.RealTimeCountResponseDto;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryRealTimeCountUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics/realtime")
@Tag(name = "Real-Time Counts", description = "Active visitors and platform users")
public class RealTimeCountRestController {

    private final QueryRealTimeCountUseCase queryRealTimeCountUseCase;
    private final AuthenticatedScopeResolver scopeResolver;
    private final DtoMapper dtoMapper;

    public RealTimeCountRestController(
            QueryRealTimeCountUseCase queryRealTimeCountUseCase,
            AuthenticatedScopeResolver scopeResolver,
            DtoMapper dtoMapper
    ) {
        this.queryRealTimeCountUseCase = queryRealTimeCountUseCase;
        this.scopeResolver = scopeResolver;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping("/{metricCode}")
    @Operation(summary = "Query the current count of active visitors or users")
    public ResponseEntity<RealTimeCountResponseDto> query(
            @Parameter(description = "Supported real-time metric code", required = true)
            @PathVariable MetricCode metricCode
    ) {
        var scope = scopeResolver.resolve(metricCode);
        var result = queryRealTimeCountUseCase.query(metricCode, scope);
        return ResponseEntity.ok(dtoMapper.toDto(result));
    }
}
