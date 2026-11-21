package edu.dosw.styleradar.estadisticas.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pending publication count per publisher")
public record PendingModerationPublisherDto(
        @Schema(description = "Publisher identifier", example = "store-123")
        String publisherId,
        @Schema(description = "Publisher name", example = "Style Store")
        String publisherName,
        @Schema(description = "Number of pending publications", example = "4")
        long pendingCount
) {
}
