package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.Objects;

public record ModerationDecisionResult(
        CategoryCode publisherType,
        String targetId,
        String resultadoModeracion,
        long moderatedCount,
        PendingModeration updatedPending
) {

    public ModerationDecisionResult {
        Objects.requireNonNull(publisherType, "Publisher type must not be null");
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("Target id must not be blank");
        }
        if (resultadoModeracion == null || resultadoModeracion.isBlank()) {
            throw new IllegalArgumentException("Moderation result must not be blank");
        }
        if (moderatedCount < 0) {
            throw new IllegalArgumentException("Moderated count must not be negative");
        }
        Objects.requireNonNull(updatedPending, "Updated pending moderation must not be null");
    }
}
