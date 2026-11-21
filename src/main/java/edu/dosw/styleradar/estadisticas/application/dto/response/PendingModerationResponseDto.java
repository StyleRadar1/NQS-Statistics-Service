package edu.dosw.styleradar.estadisticas.application.dto.response;

import edu.dosw.styleradar.estadisticas.domain.model.PendingModeration;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Publications pending moderation grouped by publisher type and publisher")
public record PendingModerationResponseDto(
        @Schema(description = "Total number of pending publications", example = "12")
        long totalPendingCount,
        @Schema(description = "Groups by publisher type")
        List<PendingModerationGroupDto> groups,
        @Schema(description = "Informational message when there are no pending publications",
                example = "No hay publicaciones pendientes de moderación")
        String message
) {

    public static PendingModerationResponseDto from(PendingModeration pendingModeration) {
        List<PendingModerationGroupDto> groups = pendingModeration.groups().stream()
                .map(group -> new PendingModerationGroupDto(
                        group.publisherType().value(),
                        group.publishers().stream()
                                .map(publisher -> new PendingModerationPublisherDto(
                                        publisher.publisherId(),
                                        publisher.publisherName(),
                                        publisher.pendingCount()))
                                .toList()))
                .toList();
        return new PendingModerationResponseDto(
                pendingModeration.totalPendingCount(),
                groups,
                pendingModeration.message());
    }
}
