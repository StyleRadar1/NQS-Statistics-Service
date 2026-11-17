package edu.dosw.styleradar.estadisticas.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Publicadores agrupados por tipo de origen")
public record PendingModerationGroupDto(
        @Schema(description = "Tipo de publicador", example = "ORIGIN_STORE")
        String publisherType,
        @Schema(description = "Publicadores y sus conteos pendientes")
        List<PendingModerationPublisherDto> publishers
) {
}
