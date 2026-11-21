package edu.dosw.styleradar.estadisticas.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Publishers grouped by origin type")
public record PendingModerationGroupDto(
        @Schema(description = "Publisher type", example = "ORIGIN_STORE")
        String publisherType,
        @Schema(description = "Publishers and their pending counts")
        List<PendingModerationPublisherDto> publishers
) {
}
