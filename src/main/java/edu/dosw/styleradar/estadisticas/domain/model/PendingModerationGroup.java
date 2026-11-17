package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.List;
import java.util.Objects;

public record PendingModerationGroup(
        CategoryCode publisherType,
        List<PendingModerationPublisher> publishers
) {

    public PendingModerationGroup {
        Objects.requireNonNull(publisherType, "Publisher type must not be null");
        publishers = List.copyOf(Objects.requireNonNull(publishers, "Publishers must not be null"));
    }
}
