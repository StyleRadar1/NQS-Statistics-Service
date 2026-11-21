package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.request.ModerationDecisionRequestDto;
import edu.dosw.styleradar.estadisticas.application.dto.response.ModerationDecisionResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.response.PendingModerationResponseDto;
import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.port.in.GetPendingModerationUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.security.AuthenticatedScopeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/statistics/moderation")
@Tag(name = "Moderation", description = "Statistics and management of publications pending moderation (SR-RF-87)")
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

    @PostMapping("/decision")
    @Operation(
            summary = "Approve or reject pending publications for a publisher or publication (SR-RF-87)",
            description = "Applies the APROBAR or RECHAZAR decision, clears resolved pending items, and returns the resulting "
                    + "state (APROBADA/RECHAZADA) along with the updated list of pending items by group.")
    public ResponseEntity<ModerationDecisionResponseDto> decide(
            @Valid @RequestBody ModerationDecisionRequestDto request
    ) {
        var scope = scopeResolver.resolve(MetricCode.MODERATION_PUBLICATIONS);
        CategoryCode publisherType = resolvePublisherType(request.tipoPublicador());
        var result = getPendingModerationUseCase.decide(
                scope,
                publisherType,
                request.identificador(),
                request.decision());
        return ResponseEntity.ok(ModerationDecisionResponseDto.from(result));
    }

    private CategoryCode resolvePublisherType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            throw new IllegalArgumentException("El tipo de publicador es obligatorio");
        }
        return switch (rawType.trim().toUpperCase(Locale.ROOT)) {
            case "TIENDA", "STORE", "ORIGIN_STORE" -> CategoryCode.ORIGIN_STORE;
            case "USUARIO", "USER", "PERSONA", "ORIGIN_USER" -> CategoryCode.ORIGIN_USER;
            default -> throw new IllegalArgumentException(
                    "Tipo de publicador inválido: " + rawType + ". Valores permitidos: TIENDA, USUARIO, ORIGIN_STORE, ORIGIN_USER");
        };
    }
}
