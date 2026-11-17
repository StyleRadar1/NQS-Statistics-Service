package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.Objects;

public record PendingModerationPublisherCount(
        CategoryCode publisherType,
        String publisherId,
        String publisherName,
        long pendingCount
) {

    public PendingModerationPublisherCount {
        Objects.requireNonNull(publisherType, "Publisher type must not be null");
        if (publisherId == null || publisherId.isBlank()) {
            throw new IllegalArgumentException("Publisher id must not be blank");
        }
        if (publisherName == null || publisherName.isBlank()) {
            throw new IllegalArgumentException("Publisher name must not be blank");
        }
        if (pendingCount <= 0) {
            throw new IllegalArgumentException("Pending count must be positive");
        }
    }
}
