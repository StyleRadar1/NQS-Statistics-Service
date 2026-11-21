package edu.dosw.styleradar.estadisticas.application.dto.response;

import edu.dosw.styleradar.estadisticas.domain.model.ModerationDecisionResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Moderation decision result and updated list of pending publications (SR-RF-87)")
public record ModerationDecisionResponseDto(
        @Schema(description = "Moderated publisher type", example = "ORIGIN_STORE")
        String tipoPublicador,
        @Schema(description = "Identifier of the moderated publisher or publication", example = "store-1")
        String identificador,
        @Schema(description = "Resulting state after applying the decision", example = "APROBADA")
        String resultadoModeracion,
        @Schema(description = "Number of resolved pending publications", example = "3")
        long moderatedCount,
        @Schema(description = "Updated list of pending publications grouped by publisher")
        PendingModerationResponseDto pendientesPorGrupo
) {

    public static ModerationDecisionResponseDto from(ModerationDecisionResult result) {
        return new ModerationDecisionResponseDto(
                result.publisherType().value(),
                result.targetId(),
                result.resultadoModeracion(),
                result.moderatedCount(),
                PendingModerationResponseDto.from(result.updatedPending()));
    }
}
