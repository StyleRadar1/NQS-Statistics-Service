package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.response.PendingModerationResponseDto;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.port.in.GetPendingModerationUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics/moderation")
@Tag(name = "Moderation", description = "Read-only pending publication moderation statistics")
public class PendingModerationRestController {

    private final GetPendingModerationUseCase getPendingModerationUseCase;
    private final AuthenticatedScopeResolver scopeResolver;

    public PendingModerationRestController(
            GetPendingModerationUseCase getPendingModerationUseCase,
            AuthenticatedScopeResolver scopeResolver
    ) {
        this.getPendingModerationUseCase = getPendingModerationUseCase;
        this.scopeResolver = scopeResolver;
    }

    @GetMapping("/pending")
    @Operation(summary = "Query pending publications grouped by publisher type and publisher")
    public ResponseEntity<PendingModerationResponseDto> queryPending() {
        var scope = scopeResolver.resolve(MetricCode.MODERATION_PUBLICATIONS);
        var result = getPendingModerationUseCase.query(scope);
        return ResponseEntity.ok(PendingModerationResponseDto.from(result));
    }
}
