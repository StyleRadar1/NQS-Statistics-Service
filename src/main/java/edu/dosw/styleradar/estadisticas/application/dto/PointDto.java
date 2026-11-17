package edu.dosw.styleradar.estadisticas.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PointDto(LocalDate date, BigDecimal value) {
}
