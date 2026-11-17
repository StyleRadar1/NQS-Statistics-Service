package edu.dosw.styleradar.estadisticas.application.dto.response;

import edu.dosw.styleradar.estadisticas.domain.model.PendingModeration;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Publicaciones pendientes de moderación agrupadas por tipo y publicador")
public record PendingModerationResponseDto(
        @Schema(description = "Número total de publicaciones pendientes", example = "12")
        long totalPendingCount,
        @Schema(description = "Grupos por tipo de publicador")
        List<PendingModerationGroupDto> groups,
        @Schema(description = "Mensaje informativo cuando no hay publicaciones pendientes",
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
