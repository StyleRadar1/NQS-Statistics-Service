package edu.dosw.styleradar.estadisticas.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Donation requests for the last 6 calendar months separated by origin (SR-RF-83)")
public record MonthlyDonationRequestsResponseDto(
        @Schema(description = "Authenticated foundation identifier", example = "foundation-1")
        String foundationId,
        @Schema(description = "Monthly count of requests submitted by individuals (6 months)")
        List<MonthlyCountDto> solicitudesPersonas,
        @Schema(description = "Monthly count of requests submitted by stores (6 months)")
        List<MonthlyCountDto> solicitudesTiendas
) {
}
