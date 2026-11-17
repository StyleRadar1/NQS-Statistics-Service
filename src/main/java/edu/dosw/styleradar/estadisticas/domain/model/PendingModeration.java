package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.List;
import java.util.Objects;

public record PendingModeration(List<PendingModerationGroup> groups, long totalPendingCount, String message) {

    public PendingModeration {
        groups = List.copyOf(Objects.requireNonNull(groups, "Groups must not be null"));
        if (totalPendingCount < 0) {
            throw new IllegalArgumentException("Total pending count must not be negative");
        }
        if (totalPendingCount == 0 && (message == null || message.isBlank())) {
            throw new IllegalArgumentException("An empty result must include a message");
        }
    }
}
