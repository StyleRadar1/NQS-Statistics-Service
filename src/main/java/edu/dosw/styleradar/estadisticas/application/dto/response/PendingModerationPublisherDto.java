package edu.dosw.styleradar.estadisticas.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Conteo de publicaciones pendientes por publicador")
public record PendingModerationPublisherDto(
        @Schema(description = "Identificador del publicador", example = "store-123")
        String publisherId,
        @Schema(description = "Nombre del publicador", example = "Tienda Style")
        String publisherName,
        @Schema(description = "Número de publicaciones pendientes", example = "4")
        long pendingCount
) {
}
