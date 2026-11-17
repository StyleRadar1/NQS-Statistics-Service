package edu.dosw.styleradar.estadisticas.application.dto;

import java.math.BigDecimal;

public record DistributionEntryDto(String categoryCode, long count, BigDecimal percentage) {
}
