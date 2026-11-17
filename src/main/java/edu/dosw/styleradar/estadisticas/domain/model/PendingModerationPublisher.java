package edu.dosw.styleradar.estadisticas.domain.model;

public record PendingModerationPublisher(String publisherId, String publisherName, long pendingCount) {

    public PendingModerationPublisher {
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
