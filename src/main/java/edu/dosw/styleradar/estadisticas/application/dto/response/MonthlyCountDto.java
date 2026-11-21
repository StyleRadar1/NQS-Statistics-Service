package edu.dosw.styleradar.estadisticas.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Count recorded in a specific calendar month")
public record MonthlyCountDto(
        @Schema(description = "Calendar month in YYYY-MM format", example = "2026-11")
        String mes,
        @Schema(description = "Total count recorded in the month", example = "14")
        BigDecimal cantidad
) {
}
