package edu.dosw.styleradar.estadisticas.application.dto.request;

import edu.dosw.styleradar.estadisticas.domain.model.ModerationDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Moderation decision request for a publisher or pending publication (SR-RF-87)")
public record ModerationDecisionRequestDto(
        @NotBlank(message = "El tipo de publicador es obligatorio")
        @Schema(description = "Publisher type: TIENDA, ORIGIN_STORE, USUARIO, or ORIGIN_USER", example = "ORIGIN_STORE")
        String tipoPublicador,
        @NotBlank(message = "El identificador del grupo o publicación es obligatorio")
        @Schema(description = "Identifier of the publisher or publication to moderate", example = "store-1")
        String identificador,
        @NotNull(message = "La decisión de moderación es obligatoria")
        @Schema(description = "Decision to apply: APROBAR or RECHAZAR", example = "APROBAR")
        ModerationDecision decision,
        @Schema(description = "Optional reason for the decision", example = "Cumple políticas de publicación")
        String motivo
) {
}
